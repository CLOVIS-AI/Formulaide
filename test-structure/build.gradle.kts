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
                api(kotlin("test"))

                api("opensavvy.pedestal:state:_")
                api("opensavvy.pedestal:state-arrow:_")
                api("opensavvy.pedestal:state-coroutines:_")

                api(KotlinX.datetime)

                api(KotlinX.coroutines.test)
                api(KotlinX.coroutines.debug)

                api(Testing.Kotest.assertions.core)
            }
        }

        val jvmMain by getting {
            dependencies {
                api(Kotlin.test.junit5)
            }
        }
    }
}
