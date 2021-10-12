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

				implementation("io.ktor:ktor-client-core:1.6.1")
				implementation("io.ktor:ktor-client-serialization:1.6.1")
				implementation("io.ktor:ktor-client-json:1.6.1")
				implementation("io.ktor:ktor-client-logging:1.6.1")
				implementation("io.ktor:ktor-client-auth:1.6.1")

				implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
			}
		}

		val jvmMain by getting {
			dependencies {
				implementation("io.ktor:ktor-client-apache:1.6.1")
				implementation("ch.qos.logback:logback-classic:1.2.4")
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
