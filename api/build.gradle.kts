plugins {
	kotlin("multiplatform")
	kotlin("plugin.serialization")

	id("org.jetbrains.dokka")
}

kotlin {
	jvm()
	js {
		browser()
	}

	@kotlin.Suppress("UNUSED_VARIABLE")
	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib-common"))

				api(kotlinxSerialization("json"))
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
