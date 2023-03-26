plugins {
    id("java")
    application
}

group = "io.github.madethoughts"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
    annotationProcessor(project(":processor"))

    implementation(project(":processor"))
    implementation("org.tomlj", "tomlj", "1.1.0")
    implementation("org.slf4j", "slf4j-api", "2.0.7")
    implementation("com.google.code.gson:gson:2.10.1")

    // adventure
    implementation("net.kyori", "adventure-api", "4.13.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.13.0")
    implementation("net.kyori", "adventure-text-minimessage", "4.13.0")

    runtimeOnly("ch.qos.logback", "logback-classic", "1.4.6")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
}

val ENABLE_PREVIEW = listOf(
    "--enable-preview"
)
application {
    mainModule.set("io.github.madethoughts.hope")
    mainClass.set("io.github.madethoughts.hope.Application")
    applicationDefaultJvmArgs = ENABLE_PREVIEW
}

tasks {
    compileJava {
        options.compilerArgs = ENABLE_PREVIEW
        modularity.inferModulePath.set(true)
    }

    test {
        useJUnitPlatform()
    }
}