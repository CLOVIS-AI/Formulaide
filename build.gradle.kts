plugins {
	// Declare Kotlin plugins for subprojects, without applying them
	kotlin("multiplatform") version Version.kotlin apply false
	kotlin("jvm") version Version.kotlin apply false
	kotlin("plugin.serialization") version Version.kotlin apply false
}

group = "fr.ville-arcachon"
version = "1.0-SNAPSHOT"

subprojects {
	group = rootProject.group
	version = rootProject.version
}
