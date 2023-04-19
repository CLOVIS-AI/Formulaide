@file:Suppress("UNUSED_VARIABLE")

import java.net.URL

plugins {
	kotlin("multiplatform")
}

kotlin {
	jvm()
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
				api("opensavvy.pedestal:backbone:_")
				api("opensavvy.pedestal:state-arrow:_")
				api(KotlinX.datetime)
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
			remoteUrl.set(URL("https://gitlab.com/opensavvy/formulaide/-/blob/main/core/src"))
			remoteLineSuffix.set("#L")
		}
	}
}

tasks.named<Test>("jvmTest") {
	useJUnitPlatform()
}
