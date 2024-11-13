buildscript {
  val kotlinVersion by extra("1.9.24")
  val coroutinesVersion by extra("1.9.0")
  dependencies {
    classpath("com.android.tools.build:gradle:8.7.2")
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${kotlinVersion}")
    classpath("com.google.firebase:firebase-crashlytics-gradle:3.0.2")
    classpath("com.google.gms:google-services:4.4.2")
  }
}

plugins {
  id("com.google.devtools.ksp") version "1.9.24-1.0.20" apply false
}

allprojects {
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://maven.google.com/")
      name = "Google"
    }
    maven {
      url = uri("https://maven.fabric.io/public")
    }
  }
}

tasks.register<Delete>("clean") {
  delete(rootProject.layout.buildDirectory)
}
