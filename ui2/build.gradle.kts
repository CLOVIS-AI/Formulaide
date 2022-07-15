plugins {
	kotlin("js")
	kotlin("plugin.serialization")
	id("org.jetbrains.compose")
}

repositories {
	mavenCentral()
	maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
	google()
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

	implementation(compose.web.core)
	implementation(compose.runtime)

	implementation(KotlinX.coroutines.core)

	implementation(npm("remixicon", "2.5.0"))

	implementation(npm("postcss", "8.4.12"))
	implementation(npm("postcss-loader", "6.2.1"))
	implementation(npm("autoprefixer", "10.4.4"))
	implementation(npm("tailwindcss", "3.0.23"))

	implementation(project(":core"))
	implementation(project(":client"))
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

tasks.named("compileKotlinJs") {
	dependsOn(copyTailwindConfig)
	dependsOn(copyPostcssConfig)
}
