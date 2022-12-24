import java.net.URL

plugins {
	// Declare Kotlin plugins for subprojects, without applying them
	kotlin("multiplatform") apply false
	kotlin("jvm") apply false
	kotlin("js") apply false
	kotlin("plugin.serialization") apply false

	id("com.palantir.git-version")

	id("org.jetbrains.dokka")
	id("org.jetbrains.kotlinx.kover")
}

group = "fr.ville-arcachon"
version = calculateVersion()

subprojects {
	group = rootProject.group
	version = rootProject.version
}

allprojects {
	plugins.apply("org.jetbrains.dokka")
	plugins.apply("org.jetbrains.kotlinx.kover")

	repositories {
		mavenCentral()
		maven("https://gitlab.com/api/v4/projects/37325377/packages/maven")
	}

	tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
		dokkaSourceSets.configureEach {
			externalDocumentationLink {
				url.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/"))
			}
			externalDocumentationLink {
				url.set(URL("https://opensavvy.gitlab.io/pedestal/documentation/"))
			}
		}
	}
}

koverMerged.enable()

fun calculateVersion(): String {
	val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
	val details = versionDetails()

	return if (details.commitDistance == 0)
		details.lastTag
	else
		"${details.lastTag}-post.${details.commitDistance}+${details.gitHash}"
}
