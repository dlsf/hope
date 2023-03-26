plugins {
    id("java")
}

group = "io.github.madethoughts.hope"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.tomlj", "tomlj", "1.1.0")
    implementation("com.squareup", "javapoet", "1.13.0")

    implementation("net.kyori", "adventure-api", "4.13.0")
    implementation("net.kyori", "adventure-text-minimessage", "4.13.0")
}