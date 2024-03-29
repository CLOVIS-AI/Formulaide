@file:Suppress("UNUSED_VARIABLE")

import java.net.URL

plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")
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
				api(projects.core.coreData)
				api(projects.core.coreUsers)
				api(projects.core.coreDomain)

				api("opensavvy.pedestal:spine:_")
				api(KotlinX.serialization.core)
				api(KotlinX.serialization.json)
			}
		}

		val commonTest by getting {
			dependencies {
				implementation(projects.test)
				implementation(projects.fake)
			}
		}
	}
}

tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
	dokkaSourceSets.configureEach {
		includes.from("${project.projectDir}/README.md")

		sourceLink {
			localDirectory.set(file("src"))
			remoteUrl.set(URL("https://gitlab.com/opensavvy/formulaide/-/blob/main/remote-common/src"))
			remoteLineSuffix.set("#L")
		}
	}
}
