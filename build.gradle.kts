plugins {
    id("java")
}

group = "com.qiuchris"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.seleniumhq.selenium:selenium-java:4.16.1")
    implementation("org.bytedeco:javacv-platform:1.5.10")
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}