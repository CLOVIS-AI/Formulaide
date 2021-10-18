plugins {
	kotlin("js")

	id("org.jetbrains.dokka")

	id("org.gradlewebtools.minify")
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

	implementation("org.jetbrains.kotlin-wrappers:kotlin-react:_")
	implementation("org.jetbrains.kotlin-wrappers:kotlin-react-dom:_")
	implementation(npm("react", "17.0.2"))
	implementation(npm("react-dom", "17.0.2"))
	implementation(npm("use-error-boundary", "2.0.6"))

	implementation(npm("postcss", "8.3.9"))
	implementation(npm("postcss-loader", "6.1.1"))
	implementation(npm("autoprefixer", "10.3.7"))
	implementation(npm("tailwindcss", "2.2.16"))

	implementation(KotlinX.coroutines.coreJs)
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
