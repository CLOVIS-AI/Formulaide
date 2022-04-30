plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("application")

	id("org.jetbrains.dokka")

	id("jacoco")
	id("de.jansauer.printcoverage")
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	implementation(project(":api"))
	implementation(project(":database"))

	implementation(Ktor.server.core)
	implementation(Ktor.server.netty)
	implementation(Ktor.features.htmlBuilder)
	implementation(Ktor.features.serialization)
	implementation(KotlinX.serialization.json)
	testImplementation("io.ktor:ktor-server-tests:_")
	implementation("ch.qos.logback:logback-classic:_")

	implementation(Ktor.features.auth)
	implementation(Ktor.features.authJwt)
	implementation("at.favre.lib:bcrypt:_")

	implementation(KotlinX.coroutines.core)
	testImplementation(KotlinX.coroutines.test)

	implementation("org.apache.tika:tika-core:_")
}

application {
	mainClass.set("formulaide.server.MainKt")
}

tasks.named<JavaExec>("run") {
	description = "Runs this project as a JVM application (development mode)"

	jvmArgs = listOf("-Dio.ktor.development=true")
}

jacoco {
	toolVersion = "0.8.8"
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)

	reports {
		xml.isEnabled = true
		csv.isEnabled = false
		html.isEnabled = true
	}
}

tasks.create<Copy>("copyFrontend") {
	dependsOn(":ui:browserProductionWebpack")

	from("${project(":ui").buildDir}/distributions")
	into("${project.buildDir}/resources/main/front")
	exclude("ui.js", "ui.js.map")
}

tasks.create<Copy>("copyFrontendJs") {
	dependsOn(":ui:browserProductionWebpack", ":ui:jsMinify")

	from("${project(":ui").buildDir}/distributions-minified")
	into("${project.buildDir}/resources/main/front")
	include("ui.min.js")
	rename { "ui.js" }
}

tasks.processResources {
	if (!project.hasProperty("devMode"))
		dependsOn("copyFrontend", "copyFrontendJs")
}
