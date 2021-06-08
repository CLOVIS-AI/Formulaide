plugins {
	kotlin("multiplatform")

	id("org.jetbrains.dokka")
}

repositories {
	mavenCentral()
}

kotlin {
	js {
		browser {
			testTask {
				useKarma {
					useChromiumHeadless()
				}
			}
		}
	}

	@kotlin.Suppress("UNUSED_VARIABLE")
	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib-common"))

				api(project(":api"))

				implementation(ktorClient("core"))
				implementation(ktorClient("serialization"))
				implementation(ktorClient("json"))
				implementation(ktorClient("logging"))
				implementation(ktorClient("auth"))
			}
		}

		val commonTest by getting {
			dependencies {
				implementation(kotlin("test-common"))
				implementation(kotlin("test-annotations-common"))
			}
		}

		val jsTest by getting {
			dependencies {
				implementation(kotlin("test-js"))
			}
		}
	}
}
