plugins {
    id("java")
    `maven-publish`
    signing
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25

    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
}

publishing {
    publications.create<MavenPublication>("maven") {
        groupId = "net.worldseed.multipart"
        artifactId = "WorldSeedEntityEngine"
        version = "13.0.3"

        from(components["java"])
    }

    repositories {
        maven {
            name = "AtlasEngine"
            url = uri("https://reposilite.atlasengine.ca/public")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:6.0.0-M2"))
    testImplementation(libs.junit.api)
    testRuntimeOnly(libs.junit.engine)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    compileOnly(libs.minestom)
    testImplementation(libs.minestom)

    implementation(libs.commons.io)
    implementation(libs.zt.zip)

    implementation(libs.javax.json.api)
    implementation(libs.javax.json)
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("demoServer") {
    group = "demo"
    description = "Runs the example Minestom demo server (src/test/java/Main.java)"
    mainClass.set("Main")
    classpath = sourceSets["test"].runtimeClasspath
    jvmArgs("--add-opens", "java.base/java.lang=ALL-UNNAMED")
}
