plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}
java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.0")
}
