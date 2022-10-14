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

	@Suppress("UNUSED_VARIABLE")
	sourceSets {
		val commonMain by getting {
			dependencies {
				api(project(":core"))
				implementation(project(":api"))

				implementation("opensavvy:spine-ktor-client:_")

				implementation(Ktor.client.core)
				implementation("io.ktor:ktor-client-content-negotiation:_")
				implementation("io.ktor:ktor-serialization-kotlinx-json:_")
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
