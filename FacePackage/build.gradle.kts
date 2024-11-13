plugins {
  id("com.android.application")
  id("kotlin-android")
}

android {
  compileSdk = 35
  defaultConfig {
    versionCode = 1
    versionName = "1.0"
    applicationId = "com.j.jface"
    minSdk = 28
  }
  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android.txt"))
    }
  }
  sourceSets {
    getByName("main") {
      java.srcDirs("src/main/java")
      res.srcDirs("src/main/res")
    }
    getByName("androidTest") {
      setRoot("tests")
      java.srcDirs("tests/src")
    }
  }

  lint {
    abortOnError = true
    checkReleaseBuilds = false
  }
  namespace = "com.j.jface"
}

dependencies {
  implementation(project(":Common"))
  implementation("com.google.android.gms:play-services-wearable:18.2.0")
  implementation("androidx.annotation:annotation:1.9.1")
  implementation("androidx.legacy:legacy-support-v13:1.0.0")
  compileOnly("com.google.android.wearable:wearable:2.9.0")
  implementation("com.google.android.support:wearable:2.9.0")
  implementation(project(path = ":Face"))
}
repositories {
  mavenCentral()
}
