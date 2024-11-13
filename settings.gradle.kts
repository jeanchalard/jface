pluginManagement {
  repositories {
    google()
    mavenCentral()
    maven {
      url = uri("https://maven.google.com/")
      name = "Google"
    }
  }
}

include(":Feed", ":FacePackage", ":Common")
include(":Face")
