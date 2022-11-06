plugins {
    id("java")
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "net.worldseed.multipart"
            artifactId = "WorldSeedEntityEngine"
            version = "1.0"

            from(components["java"])
        }
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    implementation("com.github.Minestom:Minestom:42195c536b")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.zeroturnaround:zt-zip:1.8")

    implementation("javax.json:javax.json-api:1.1.4")
    implementation("org.glassfish:javax.json:1.1.4")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
