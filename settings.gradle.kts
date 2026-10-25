pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        // JitPack hosts NewPipe Extractor (built from TeamNewPipe/NewPipeExtractor GitHub tags).
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "spoyt"
include(":app")
