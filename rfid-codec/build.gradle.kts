plugins {
    kotlin("jvm")
    `java-library`
    `maven-publish`
}

description = "RFID tag codec for XingZhi library applications (ISO 28560 HF decode + CULTU UHF EPC encode/decode)"

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        javaParameters.set(true)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/OpenXingZhi/rfid-codec")
            credentials {
                username = providers.gradleProperty("GitHubPackagesUsername")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR"))
                    .getOrElse("")
                password = providers.gradleProperty("GitHubPackagesPassword")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN"))
                    .getOrElse("")
            }
        }
    }
    publications {
        create<MavenPublication>("library") {
            artifactId = "rfid-codec"
            from(components["java"])
            pom {
                name.set("XingZhi RFID Codec")
                description.set(project.description)
                url.set("https://github.com/OpenXingZhi/rfid-codec")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                scm {
                    url.set("https://github.com/OpenXingZhi/rfid-codec")
                    connection.set("scm:git:https://github.com/OpenXingZhi/rfid-codec.git")
                }
            }
        }
    }
}
