module io.github.madethoughts.hope {
    uses io.github.madethoughts.hope.configuration.NetworkingConfig;

    requires org.slf4j;
    requires java.net.http;
    requires org.tomlj;
    requires com.google.gson;

    // adventure ----
    requires net.kyori.adventure;
    requires net.kyori.adventure.text.minimessage;
    requires net.kyori.examination.string;
    // adventure dependencies
    requires net.kyori.adventure.text.serializer.gson;
    requires java.sql;

    requires io.github.madethoughts.hope.processor;

    exports io.github.madethoughts.hope.network.packets.clientbound.status to com.google.gson;
}