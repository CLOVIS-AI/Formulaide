plugins {
	kotlin("js")

	id("org.jetbrains.dokka")

	id("org.gradlewebtools.minify") version Version.gradleMinify
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

tasks.named("compileKotlinJs") {
	dependsOn(copyTailwindConfig)
	dependsOn(copyPostcssConfig)
}

// Fix for https://youtrack.jetbrains.com/issue/KT-48273
rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin> {
	rootProject.the<org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension>().versions.webpackDevServer.version =
		"4.0.0-rc.0"
}

val jsMinify by tasks.creating(org.gradlewebtools.minify.JsMinifyTask::class.java) {
	dependsOn("browserProductionWebpack")

	srcDir = project.buildDir / "distributions"
	dstDir = project.buildDir / "distributions-minified"

	options {
		compilationLevel = com.google.javascript.jscomp.CompilationLevel.SIMPLE_OPTIMIZATIONS
		env = com.google.javascript.jscomp.CompilerOptions.Environment.BROWSER
	}
}

operator fun File.div(childName: String) = File(this, childName)
