import java.time.Duration

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
					timeout.set(Duration.ofMinutes(1))
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

				implementation(kotlinxSerialization("core"))
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
