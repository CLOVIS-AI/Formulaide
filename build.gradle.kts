plugins {
	// Declare Kotlin plugins for subprojects, without applying them
	kotlin("multiplatform") apply false
	kotlin("jvm") apply false
	kotlin("js") apply false
	kotlin("plugin.serialization") apply false

	id("org.jetbrains.dokka")
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
