@file:Suppress("UNUSED_VARIABLE")

plugins {
	kotlin("multiplatform")
}

kotlin {
	jvm {
		jvmToolchain(17)
		testRuns.named("test") {
			executionTask.configure {
				useJUnitPlatform()
			}
		}
	}
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
		all {
			languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
			languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
		}

		val commonMain by getting {
			dependencies {
				api(projects.core.coreData)
				api(projects.core.coreUsers)
				api(projects.core.coreDomain)
				api(projects.testStructure)
			}
		}

		val jvmMain by getting {
			dependencies {
				implementation("org.jetbrains.kotlin:kotlin-reflect:_")
				implementation("ch.qos.logback:logback-classic:_")
			}
		}
	}
}
