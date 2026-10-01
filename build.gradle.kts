buildscript {
    dependencies {
        // AGP 9.x has built-in Kotlin. Pin a newer KGP runtime explicitly
        // so the compiler and Compose compiler stay on Kotlin 2.4.20.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
}
