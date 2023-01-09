@file:Suppress("UNUSED_VARIABLE")

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
				api(projects.core)

				implementation("opensavvy:logger:_")
			}
		}

		val commonTest by getting {
			dependencies {
				implementation(projects.test)
			}
		}
	}
}