@file:Suppress("UNUSED_VARIABLE")

import java.net.URL

plugins {
	kotlin("multiplatform")
}

kotlin {
	jvm {
		jvmToolchain(17)
		testRuns.named("test") {
			executionTask.configure {
				useJUnitPlatform()
			}
		}
	}
	js(IR) {
		browser {
			testTask {
				useMocha {
					timeout = "1 minute"
				}
			}
		}
	}

	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(projects.remoteCommon)
				implementation("opensavvy.pedestal:spine-ktor-client:_")
				implementation(Ktor.client.contentNegotiation)
				implementation(Ktor.plugins.serialization.kotlinx.json)

				implementation("opensavvy.pedestal:logger:_")
			}
		}

		val commonTest by getting {
			dependencies {
				implementation(projects.test)
			}
		}
	}
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
	dokkaSourceSets.configureEach {
		includes.from("${project.projectDir}/README.md")

		sourceLink {
			localDirectory.set(file("src"))
			remoteUrl.set(URL("https://gitlab.com/opensavvy/formulaide/-/blob/main/remote-client/src"))
			remoteLineSuffix.set("#L")
		}
	}
}
