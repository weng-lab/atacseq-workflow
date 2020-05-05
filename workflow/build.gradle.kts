import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.3.31"
    id("application")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "4.0.2"
}

group = "com.genomealmanac.atacseq"
version = "0.1.10"
val artifactID = "atacseq-workflow"

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("io.krews", "krews", "0.10.6")
}

application {
    mainClassName = "AtacSeqWorkflowKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    archiveBaseName.set(artifactID)
    archiveClassifier.set("exec")
    destinationDirectory.set(file("build"))
}

val publicationName = "atacseq-workflow"
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/weng-lab/atacseq-workflow")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_USER")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("gpr") {
            artifactId = artifactID
            from(components["java"])
            artifact(shadowJar)
        }
    }
}