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
        all {
            languageSettings.optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
        }

        val commonMain by getting {
            dependencies {
                api(Kotlin.test.common)
                api(Kotlin.test.annotationsCommon)

                api("opensavvy:state:_")

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
