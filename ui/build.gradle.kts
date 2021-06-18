plugins {
	kotlin("js")

	id("org.jetbrains.dokka")
}

kotlin {
	js {
		browser {
			testTask {
				useKarma {
					useChromeHeadless()
				}
			}
		}
		binaries.executable()
	}
}

dependencies {
	testImplementation(kotlin("test-js"))

	implementation(project(":client"))

	implementation(kotlinWrapper("react", Version.kotlinReact))
	implementation(kotlinWrapper("react-dom", Version.kotlinReact))
	implementation(npm("react", Version.react))
	implementation(npm("react-dom", Version.react))

	implementation(kotlinxCoroutines("core-js"))
}
