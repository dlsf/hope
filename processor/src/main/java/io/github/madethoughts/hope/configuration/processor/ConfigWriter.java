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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.tomlj.TomlTable;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Generates config implementations. Each implementation has a no args constructor and a load methods, that takes the
 * main toml config and the default (fallback) config. There are no validation checks in this implementation.
 */
public final class ConfigWriter {

    private final TomlTable defaultValues;

    private final FieldSpec tomlField;
    private final TypeSpec.Builder typeSpecBuilder;

    private final Map<FieldSpec, JavaFile> innerConfigFields = new HashMap<>();

    private final PackageElement interfacePackage;

    public ConfigWriter(TomlTable defaultValues, TypeElement interfaceElement, Elements elements) {
        this.defaultValues = defaultValues;
        this.interfacePackage = elements.getPackageOf(interfaceElement);
        this.tomlField = FieldSpec.builder(TomlTable.class, "toml", Modifier.PRIVATE)
                                  .build();
        this.typeSpecBuilder = TypeSpec.classBuilder(interfaceElement.getSimpleName() + "$Implementation")
                                       .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                                       .addSuperinterface(interfaceElement.asType())
                                       .addField(tomlField);
    }

    /**
     * Adds an implementation for a simple toml scalar value, containing an default fallback.
     *
     * @param descriptor the PropertyDescriptor of this method/value
     */
    public void addProperty(PropertyDescriptor descriptor) {
        var type = descriptor.kind();

        switch (type) {
            case INTEGER -> addGetter(descriptor, "getLong", "$L");
            case FLOAT -> addGetter(descriptor, "getDouble", "$L");
            case STRING -> addGetter(descriptor, "getString", "$S");
        }
    }

    /**
     * Adds a delegate to an inner config (toml table).
     *
     * @param element the interface method
     * @param file    the inner configs generated JavaFile
     */
    public void addDelegate(ExecutableElement element, JavaFile file) {
        var typeName = TypeName.get(element.getReturnType());
        var field = FieldSpec.builder(typeName, element.getSimpleName().toString(), Modifier.PRIVATE)
                             .build();
        innerConfigFields.put(field, file);

        var method = MethodSpec.overriding(element)
                               .addStatement("return $N", field)
                               .build();
        typeSpecBuilder.addMethod(method);
    }

    /**
     * Adds a simple getter implementation for this specific method (PropertyDescriptor)
     *
     * @param descriptor   the descriptor
     * @param getterMethod the name of the corresponding getter method in {@link TomlTable}
     */
    private void addGetter(PropertyDescriptor descriptor, String getterMethod, String defaultPlaceholder) {
        var returnType = descriptor.method().getReturnType();
        var name = descriptor.name();
        var kind = descriptor.kind();

        var defaultValue = defaultValues.get(name);
        if (!kind.rightType(defaultValue)) {
            throw new IllegalArgumentException("Default value is of wrong type. Excepted: %s, got: %s"
                    .formatted(kind, TomlKind.forClass(defaultValue.getClass())));
        }

        var method = MethodSpec.overriding(descriptor.method())
                               .addStatement("return ($T) $N.$N($S, () -> %s)".formatted(defaultPlaceholder),
                                       returnType,
                                       tomlField, getterMethod,
                                       name, defaultValues.get(name)
                               )
                               .build();
        typeSpecBuilder.addMethod(method);
    }

    /**
     * Generates and writes the final JavaFile for this ConfigWriter.
     *
     * @param filer the annotation processor Filer
     * @return the generated JavaFile
     * @throws IOException any I/O Exception
     */
    public JavaFile generate(Filer filer) throws IOException {
        var loadMethod = MethodSpec.methodBuilder("load")
                                   .addModifiers(Modifier.PUBLIC)
                                   .addParameter(TomlTable.class, "toml")
                                   .addStatement("this.$N = toml", tomlField);

        int i = 0;
        for (var entry : innerConfigFields.entrySet()) {
            var field = entry.getKey();
            var file = entry.getValue();
            typeSpecBuilder.addField(field);

            var varName = "a%s".formatted(++i);
            loadMethod
                    .addStatement("var $N = new $T()", varName, ClassName.get(file.packageName, file.typeSpec.name))
                    .addStatement("$N.load($N)", varName, tomlField)
                    .addStatement("this.$N = $N", field, varName);
        }

        var spec = typeSpecBuilder
                .addMethod(loadMethod.build())
                .build();
        var file = JavaFile.builder(interfacePackage.getQualifiedName().toString(), spec)
                           .build();
        file.writeTo(filer);
        return file;
    }
}
