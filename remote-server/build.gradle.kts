@file:Suppress("UNUSED_VARIABLE")

import java.net.URL

plugins {
	kotlin("jvm")
}

dependencies {
	implementation(projects.remoteCommon)

	testImplementation(projects.test)
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
