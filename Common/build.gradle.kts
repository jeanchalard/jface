plugins {
  id("java-library")
}

java {
  targetCompatibility = JavaVersion.VERSION_21
  sourceCompatibility = JavaVersion.VERSION_21
}

dependencies {
  implementation("androidx.annotation:annotation:1.9.1")
  api(fileTree("libs") { include("*.jar") })
}
