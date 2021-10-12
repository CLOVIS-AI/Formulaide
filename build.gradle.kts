plugins {
	// Declare Kotlin plugins for subprojects, without applying them
	kotlin("multiplatform") version "1.5.21" apply false
	kotlin("jvm") version "1.5.21" apply false
	kotlin("js") version "1.5.21" apply false
	kotlin("plugin.serialization") version "1.5.21" apply false

	id("org.jetbrains.dokka") version "1.4.32"
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
