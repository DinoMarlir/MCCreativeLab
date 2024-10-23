plugins {
    java
    //id("io.papermc.paperweight.userdev") version "1.7.3"
}

description = "plugin-extension"
group = "de.verdox.mccreativelab"
version = "1.0"

allprojects {
    apply(plugin = "java")
    apply(plugin = "com.github.johnrengelman.shadow")


    java {
        // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
    dependencies {
        compileOnly(project(":mccreativelab-api"))
        implementation(project(":mcc-util"))
        implementation(project(":mcc-serializer"))

        //paperweight.devBundle("de.verdox.mccreativelab", "1.21.1-R0.1-SNAPSHOT")

        compileOnly("com.hierynomus:sshj:0.38.0")
        compileOnly("io.vertx:vertx-core:4.5.0")
        compileOnly("ws.schild:jave-all-deps:3.5.0")
        compileOnly("org.apache.commons:commons-compress:1.26.1")
        compileOnly("commons-io:commons-io:2.16.1")
        compileOnly("org.tukaani:xz:1.9")

        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
        testImplementation("org.hamcrest:hamcrest:2.2")
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://papermc.io/repo/repository/maven-public/")
        }
    }

    tasks.test {
        useJUnitPlatform()
    }

    val jarPath = "../plugin-extension/build/libs/plugin-extension-$version.jar";

    tasks {
        compileJava {
            options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

            // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
            // See https://openjdk.java.net/jeps/247 for more information.
            options.release.set(21)
        }

        named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
            archiveClassifier.set("") // Entfernt das "-all" Suffix
        }


        processResources {
            filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
        }

        val copyTask = register<Copy>("copyToTestServer") {
            dependsOn(":plugin-extension:shadowJar")

            println("Copying $jarPath to testserver")
            from(jarPath)
            into(file("../run/plugins"))
        }

        build {
            finalizedBy(copyTask)
        }
    }

    configure<PublishingExtension> {
        publications.create<MavenPublication>("maven") {
            from(components["java"])
            //artifact(tasks["shadowJar"])
        }
    }

    tasks.processResources {
        val apiVersion = rootProject.providers.gradleProperty("mcVersion").get()
            .split(".", "-").take(2).joinToString(".")
        val props = mapOf(
            "version" to project.version,
            "apiversion" to "\"$apiVersion\"",
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
