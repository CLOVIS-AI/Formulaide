plugins {
	// Declare Kotlin plugins for subprojects, without applying them
	kotlin("multiplatform") version Version.kotlinVersion apply false
	kotlin("jvm") version Version.kotlinVersion apply false
	kotlin("js") version Version.kotlinVersion apply false
	kotlin("plugin.serialization") version Version.kotlinVersion apply false
}

group = "fr.ville-arcachon"
version = "1.0-SNAPSHOT"

subprojects {
	group = rootProject.group
	version = rootProject.version
}

allprojects {
	repositories {
		mavenCentral()
	}
}
