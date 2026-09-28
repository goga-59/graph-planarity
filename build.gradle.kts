plugins {
    kotlin("jvm") version "2.4.20"
    application
}

group = "me.goga59"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass = "me.goga59.planarity.MainKt"
}