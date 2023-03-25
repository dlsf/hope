module io.github.madethoughts.hope {
    uses io.github.madethoughts.hope.configuration.NetworkingConfig;
    requires org.slf4j;
    requires org.json;
    requires java.net.http;
    requires org.tomlj;

    requires io.github.madethoughts.hope.processor;
}