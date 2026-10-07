plugins {
    kotlin("jvm") version "2.0.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.squig.equalizer.AppKt")
}

tasks.test {
    useJUnitPlatform()
}
