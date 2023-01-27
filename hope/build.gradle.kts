plugins {
    id("java")
    application
}

group = "io.github.madethoughts"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openjdk.jol", "jol-core", "0.16")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}


//java {
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(19))
//    }
//}

val ENABLE_PREVIEW = listOf(
    "--enable-preview",
    "--add-exports",
    "java.base/jdk.internal.vm.annotation=io.github.madethoughts.hope"
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