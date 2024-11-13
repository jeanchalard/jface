plugins {
  id("com.android.library")
}

android {
  compileSdk = 35

  defaultConfig {
      minSdk = 28
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
      consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  namespace = "com.j.jface.face"
}

dependencies {
  implementation(fileTree("libs") { include("*.jar") })
  implementation("androidx.appcompat:appcompat:1.7.0")
  implementation("com.google.android.gms:play-services-wearable:18.2.0")
  compileOnly("com.google.android.wearable:wearable:2.9.0")
  implementation("com.google.android.support:wearable:2.9.0")
  implementation(project(path = ":Common"))
}

java {
  toolchain {
    // Must be specified explicitly because the minSdk setting sets this to 8, which is not the same as everything else
    languageVersion = JavaLanguageVersion.of(21)
  }
}
