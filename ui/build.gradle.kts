plugins {
	kotlin("js")

	id("org.jetbrains.dokka")
}

kotlin {
	js(IR) {
		browser {
			testTask {
				useKarma {
					useChromeHeadless()
				}
			}
			commonWebpackConfig {
				cssSupport.enabled = true
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

	implementation(npm("postcss", Version.postcss))
	implementation(npm("postcss-loader", Version.postcssLoader))
	implementation(npm("autoprefixer", Version.autoprefixer))
	implementation(npm("tailwindcss", Version.tailwind))

	implementation(kotlinxCoroutines("core-js"))
}

val copyTailwindConfig = tasks.register<Copy>("copyTailwindConfig") {
	from("./tailwind.config.js")
	into("${rootProject.buildDir}/js/packages/${rootProject.name}-${project.name}")

	dependsOn(":kotlinNpmInstall")
}

val copyPostcssConfig = tasks.register<Copy>("copyPostcssConfig") {
	from("./postcss.config.js")
	into("${rootProject.buildDir}/js/packages/${rootProject.name}-${project.name}")

	dependsOn(":kotlinNpmInstall")
}

tasks.named("processResources") {
	dependsOn(copyTailwindConfig)
	dependsOn(copyPostcssConfig)
}
