import java.net.URL

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
}

dependencies {
	api(projects.core)

	api(KotlinX.coroutines.core)
	implementation("org.litote.kmongo:kmongo-coroutine-serialization:_")

	implementation("opensavvy:logger:_")

	testImplementation(projects.test)
	testImplementation(projects.fake)
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
	dokkaSourceSets.configureEach {
		includes.from("${project.projectDir}/README.md")

		sourceLink {
			localDirectory.set(file("src"))
			remoteUrl.set(URL("https://gitlab.com/opensavvy/formulaide/-/blob/main/mongo/src"))
			remoteLineSuffix.set("#L")
		}
	}
}
