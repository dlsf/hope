/*
 *     Hope - A minecraft server reimplementation
 *     Copyright (C) 2023 Nick Hensel and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.madethoughts.hope.configuration.processor;

import com.squareup.javapoet.JavaFile;
import org.tomlj.Toml;
import org.tomlj.TomlTable;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generates toml configuration implementations for interfaces annotated with @Configuration.
 * Each abstract method corresponds to a toml value of pattern: path.method_name_as_snake_case.
 * If a method returns another interface it will be implemented equally, but the method name is appended to the path
 * in snake case before. All generated methods have a fallback to the default config that is passed to the
 * implementation.
 */
@SupportedAnnotationTypes("io.github.madethoughts.hope.configuration.processor.Configuration")
@SupportedSourceVersion(SourceVersion.RELEASE_19)
public class ConfigProcessor extends AbstractProcessor {

    private final Map<Element, JavaFile> generatedClasses = new ConcurrentHashMap<>();

    private Element currentElement = null;

    private Messager messager;
    private Types types;
    private Elements elements;

    private Filer filer;

    private String currentPath;
    private ConfigWriter currentWriter;

    private TypeMirror abstractConfigType;
    private TypeMirror stringElement;

    // the default config dir, resolved assuming the default gradle project structure
    private Path defaultsDir;

    public static String camelToSnake(String str) {
        var regex = "([a-z])([A-Z]+)";
        return str
                .replaceAll(regex, "$1_$2")
                .toLowerCase();
    }

    private static <T> T unsupportedTypeException(TypeMirror mirror) {
        throw new UnsupportedOperationException(
                "The type %s is unsupported as a return type".formatted(mirror));
    }

    @Override
    public synchronized void init(ProcessingEnvironment env) {
        this.messager = env.getMessager();
        this.types = env.getTypeUtils();
        this.elements = env.getElementUtils();
        this.filer = env.getFiler();
        this.abstractConfigType = elements.getTypeElement(AbstractConfig.class.getCanonicalName()).asType();
        this.stringElement = elements.getTypeElement(String.class.getCanonicalName()).asType();

        try {
            var root = Path.of(filer.getResource(StandardLocation.CLASS_OUTPUT, "", "dummy").toUri());
            for (int i = 0; i < 5; i++) root = root.getParent();
            this.defaultsDir = Path.of(root.toString(), "src", "main", "resources", "defaults");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!annotations.contains(elements.getTypeElement(Configuration.class.getCanonicalName()))) return false;

        for (var element : roundEnv.getElementsAnnotatedWith(Configuration.class)) {
            if (element.getKind() != ElementKind.INTERFACE) {
                messager.printError("Only interfaces can be annotated.", element);
                return true;
            }

            var typeElement = (TypeElement) element;

            if (!typeElement.getInterfaces().contains(abstractConfigType)) {
                messager.printError("Interface must implement AbstractConfig", element);
                return true;
            }

            try {
                var annotation = element.getAnnotation(Configuration.class);
                var configResult = Toml.parse(defaultsDir.resolve(annotation.value()));
                if (configResult.hasErrors()) {
                    messager.printError("Default config %s contains errors: %s"
                            .formatted(annotation.value(), configResult.errors()), element);
                    continue;
                }

                // validate config version
                var interfaceVersion = annotation.version();
                var version = configResult.getLong("version");
                if (version == null) {
                    messager.printError("Default config must have version field.", element);
                    continue;
                }
                if (interfaceVersion > version) {
                    messager.printError("Default config is outdated.", element);
                    continue;
                }
                if (version > interfaceVersion) {
                    messager.printError("Interface version is outdated.", element);
                    continue;
                }

                processRoot(typeElement, null, configResult);
            } catch (Throwable e) {
                messager.printError(
                        "Exception during generation of config implementation: %s".formatted(e.toString()),
                        currentElement
                );
            }
        }

        return true;
    }

    /**
     * Generates a new interface implementation
     *
     * @param root The interface
     * @param path the curren path, null if root (@Configuration annotated class)
     * @return the generated JavaFile
     */
    private JavaFile processRoot(TypeElement root, String path, TomlTable tomlTable) {
        currentElement = root;

        var oldPath = currentPath;
        var oldWriter = currentWriter;

        return generatedClasses.computeIfAbsent(root, key -> {
            currentPath = path;
            try {
                currentWriter = new ConfigWriter(tomlTable, root, elements);

                // aggregate elements of interface and extended ones
                var aggregatedElements = new ArrayList<Element>(root.getEnclosedElements());
                var interfaceElements = root.getInterfaces()
                                            .stream()
                                            .map(types::asElement)
                                            .map(Element::getEnclosedElements)
                                            .flatMap(List::stream)
                                            .toList();
                aggregatedElements.addAll(interfaceElements);

                // compute each implementation
                for (var element : aggregatedElements) {
                    currentElement = element;

                    var modifiers = element.getModifiers();
                    if (element instanceof ExecutableElement method && !modifiers.contains(Modifier.STATIC) &&
                        !modifiers.contains(Modifier.DEFAULT)) {
                        var returnType = method.getReturnType();
                        var methodName = camelToSnake(method.getSimpleName().toString());
                        var name = currentPath != null
                                   ? "%s.%s".formatted(currentPath, methodName)
                                   : methodName;

                        // special cases for needed methods
                        if ("version".equals(name)) {
                            currentWriter.addVersionGetter(method);
                            continue;
                        }

                        if ("default_version".equals(name)) {
                            currentWriter.addDefaultVersionGetter(method);
                            continue;
                        }

                        // Map java type to corresponding toml kind
                        var tomlType = (TomlKind) switch (returnType.getKind()) {
                            case BYTE, INT, SHORT, LONG -> TomlKind.INTEGER;
                            case FLOAT, DOUBLE -> TomlKind.FLOAT;
                            case DECLARED -> {
                                if (types.isSameType(stringElement, returnType)) yield TomlKind.STRING;
                                yield null;
                            }
                            default -> unsupportedTypeException(returnType);
                        };

                        var returnElement = types.asElement(returnType);
                        if (tomlType != null) {
                            currentWriter.addProperty(new PropertyDescriptor(name, method, tomlType));
                        } else if (returnElement.getKind() == ElementKind.INTERFACE) {
                            var javaFile = processRoot((TypeElement) returnElement, name, tomlTable);
                            currentWriter.addDelegate(method, javaFile);
                        }
                    }
                }

                return currentWriter.generate(filer);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                currentPath = oldPath;
                currentWriter = oldWriter;
            }
        });
    }

}
