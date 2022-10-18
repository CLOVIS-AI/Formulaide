plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")

	id("org.jetbrains.dokka")
}

kotlin {
	jvm()
	js(IR) {
		browser()
	}

	@Suppress("UNUSED_VARIABLE")
	sourceSets {
		val commonMain by getting {
			dependencies {
				api(KotlinX.serialization.json)
				api(project(":core2"))
				api("opensavvy:spine:_")
			}
		}

		val commonTest by getting {
			dependencies {
				implementation(kotlin("test-common"))
				implementation(kotlin("test-annotations-common"))
			}
		}

		val jvmTest by getting {
			dependencies {
				implementation(kotlin("test-junit"))
			}
		}

		val jsTest by getting {
			dependencies {
				implementation(kotlin("test-js"))
			}
		}
	}
}
