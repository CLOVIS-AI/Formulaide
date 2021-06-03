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
}
