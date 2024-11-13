plugins {
  id("com.android.application")
  id("kotlin-android")
  id("com.google.firebase.crashlytics")
  id("com.google.devtools.ksp")
  id("com.google.gms.google-services")
}

android {
  compileSdk = 35
  defaultConfig {
    applicationId = "com.j.jface"
    minSdk = 32
    targetSdk = 34
    versionCode = 1
    versionName = "1.0"
    multiDexEnabled = true
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }
  sourceSets {
    getByName("main") {
      java.srcDirs("src/main/java")
      res.srcDirs("src/main/res")
    }
    getByName("androidTest") {
      resources.srcDirs("src/androidTests/res")
    }
  }
  lint {
    abortOnError = true
    checkReleaseBuilds = false
    disable += "NewApi"
  }
  namespace = "com.j.jface"

  useLibrary("android.test.runner")
  useLibrary("android.test.base")
  useLibrary("android.test.mock")
}

dependencies {
  val kotlinVersion = rootProject.extra["kotlinVersion"]
  val coroutinesVersion = rootProject.extra["coroutinesVersion"]
  val roomVersion = "2.6.1"

  implementation(project(":Common"))
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${kotlinVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${coroutinesVersion}")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${coroutinesVersion}")
  implementation("androidx.core:core-ktx:1.15.0")
  implementation("com.google.firebase:firebase-core:21.1.1")
  implementation("com.google.firebase:firebase-firestore:25.1.1")
  implementation("com.google.firebase:firebase-auth:23.1.0")
  implementation("com.google.firebase:firebase-messaging:24.0.3")
  implementation("com.google.android.gms:play-services-auth:21.2.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("com.google.firebase:firebase-crashlytics:19.2.1")
  implementation("com.google.firebase:firebase-analytics:22.1.2")
  compileOnly("com.google.android.wearable:wearable:2.9.0")
  implementation("com.google.android.support:wearable:2.9.0")
  implementation("com.google.android.gms:play-services-wearable:18.2.0")
  implementation("com.google.android.gms:play-services-location:21.3.0")
  implementation("com.google.android.gms:play-services-drive:17.0.0")
  implementation("androidx.annotation:annotation:1.9.1")
  implementation("androidx.legacy:legacy-support-v13:1.0.0")
  implementation("androidx.recyclerview:recyclerview:1.3.2")
  implementation("com.google.android.material:material:1.12.0")
  implementation("androidx.room:room-runtime:${roomVersion}")
  ksp("androidx.room:room-compiler:${roomVersion}")
  implementation("androidx.room:room-ktx:${roomVersion}")
  wearApp(project(":FacePackage"))
  implementation("androidx.appcompat:appcompat:1.7.0")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0", {
    exclude(group = "com.android.support", module = "support-annotations")
  })
  testImplementation("junit:junit:4.13-beta-3")
  implementation(project(path = ":Face"))
}
repositories {
  mavenCentral()
}

java {
  toolchain {
    // Must be specified explicitly because the minSdk setting sets this to 8, which is not the same as everything else
    languageVersion = JavaLanguageVersion.of(21)
  }
}
