plugins {
    id("java")
    id("application")
    id("com.github.johnrengelman.shadow")
}
application { mainClass.set("edu.ucla.belief.ui.UI") }

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("all")
    isZip64 = true
}

dependencies {
    implementation(project(":inflib"))
}
