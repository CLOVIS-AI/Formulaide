plugins {
	kotlin("multiplatform")

	id("org.jetbrains.dokka")
}

repositories {
	mavenCentral()
}

kotlin {
	js(IR) {
		browser {
			testTask {
				useMocha {
					timeout = "1 minute"
				}
			}
		}
	}
	jvm()

	@kotlin.Suppress("UNUSED_VARIABLE")
	sourceSets {
		val commonMain by getting {
			dependencies {
				implementation(kotlin("stdlib-common"))

				api(project(":api"))

				implementation(Ktor.client.core)
				implementation(Ktor.client.serialization)
				implementation(Ktor.client.json)
				implementation(Ktor.client.logging)
				implementation(Ktor.client.auth)

				implementation(KotlinX.serialization.json)
			}
		}

		val jvmMain by getting {
			dependencies {
				implementation(Ktor.client.apache)
				implementation("ch.qos.logback:logback-classic:_")
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
