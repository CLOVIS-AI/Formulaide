@file:Suppress("UNUSED_VARIABLE")

import java.net.URL

plugins {
	kotlin("jvm")
}

dependencies {
	implementation(projects.remoteCommon)
	implementation("opensavvy:spine-ktor-server:_")
	implementation(Ktor.server.contentNegotiation)
	implementation(Ktor.plugins.serialization.kotlinx.json)

	implementation("opensavvy:logger:_")

	testImplementation(projects.test)
	testImplementation(projects.remoteClient)
	testImplementation(projects.fake)
	testImplementation(Ktor.server.testHost)
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
	dokkaSourceSets.configureEach {
		includes.from("${project.projectDir}/README.md")

		sourceLink {
			localDirectory.set(file("src"))
			remoteUrl.set(URL("https://gitlab.com/opensavvy/formulaide/-/blob/main/remote-server/src"))
			remoteLineSuffix.set("#L")
		}
	}
}
