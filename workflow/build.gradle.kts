import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.jfrog.bintray.gradle.BintrayExtension

plugins {
    kotlin("jvm") version "1.3.11"
    id("application")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "4.0.2"
    id("com.jfrog.bintray") version "1.8.1"
}

group = "com.genomealmanac.atacseq"
version = "0.1.0"
val artifactID = "atacseq-workflow"

repositories {
    jcenter()
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("io.krews", "krews", "0.4.7")
}

application {
    mainClassName = "AtacSeqWorkflowKt"
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

val shadowJar: ShadowJar by tasks
shadowJar.apply {
    baseName = artifactID
    classifier = "exec"
    destinationDir = file("build")
}

// Bits for publishing the following to Bintray: default, with sources, and executable shadowJar
val sourcesJar by tasks.creating(Jar::class) {
    classifier = "sources"
    from(java.sourceSets["main"].allSource)
}

val publicationName = "atacseq-workflow"
publishing {
    publications.invoke {
        publicationName(MavenPublication::class) {
            artifactId = artifactID
            from(components["java"])
            artifact(sourcesJar)
            artifact(shadowJar)
        }
    }
}

bintray {
    user = if(hasProperty("bintrayUser")) property("bintrayUser") as String? else System.getenv("BINTRAY_USER")
    key = if(hasProperty("bintrayKey")) property("bintrayKey") as String? else System.getenv("BINTRAY_KEY")
    setPublications(publicationName)
    publish = true
    override = true
    dryRun = true
    pkg(delegateClosureOf<BintrayExtension.PackageConfig> {
        repo = "maven"
        name = "atacseq-workflow"
        githubRepo = "weng-lab/atacseq-workflow"
        vcsUrl = "https://github.com/weng-lab/atacseq-workflow"
        setLabels("kotlin")
        setLicenses("MIT")
    })
}