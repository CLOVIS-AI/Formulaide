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
				implementation(projects.fake)

				api(Kotlin.test.common)
				api(Kotlin.test.annotationsCommon)

				api(KotlinX.coroutines.test)
				api(KotlinX.coroutines.debug)
			}
		}

		val jvmMain by getting {
			dependencies {
				api(Kotlin.test.junit5)

				implementation("org.jetbrains.kotlin:kotlin-reflect:_")
				implementation("ch.qos.logback:logback-classic:_")
			}
		}

		val jsMain by getting {
			dependencies {
				api(Kotlin.test.js)
			}
		}
	}
}

tasks.named<Test>("jvmTest") {
	useJUnitPlatform()
}
