plugins {
	kotlin("js")
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
}
