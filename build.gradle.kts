plugins {
    id("java")
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

group = "net.worldseed"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.worldseed"
            artifactId = "WSEE"
            version = "1.0-SNAPSHOT"
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("com.github.Minestom:Minestom:bfa2dbd3f7")
    implementation("commons-io:commons-io:2.11.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}