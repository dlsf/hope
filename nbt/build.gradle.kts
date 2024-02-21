plugins {
    id("java")
    application
}

group = "io.github.madethoughts.hope"
version = "0.1"

repositories {
    mavenCentral()
}

dependencies {
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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