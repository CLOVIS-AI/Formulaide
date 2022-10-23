plugins {
	// Declare Kotlin plugins for subprojects, without applying them
	kotlin("multiplatform") apply false
	kotlin("jvm") apply false
	kotlin("js") apply false
	kotlin("plugin.serialization") apply false

	id("com.palantir.git-version")

	id("org.jetbrains.dokka")
}

group = "fr.ville-arcachon"
version = calculateVersion()

subprojects {
	group = rootProject.group
	version = rootProject.version
}

allprojects {
	repositories {
		mavenCentral()
		maven("https://gitlab.com/api/v4/projects/37325377/packages/maven")
	}
}

fun calculateVersion(): String {
	val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
	val details = versionDetails()

	return if (details.commitDistance == 0)
		details.lastTag
	else
		"${details.lastTag}-post.${details.commitDistance}+${details.gitHash}"
}
