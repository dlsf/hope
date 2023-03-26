module io.github.madethoughts.hope.processor {
    requires java.compiler;
    requires com.squareup.javapoet;
    requires org.tomlj;
    requires net.kyori.adventure.text.minimessage;
    requires net.kyori.adventure;
    requires net.kyori.examination.api;

    exports io.github.madethoughts.hope.configuration.processor;
}