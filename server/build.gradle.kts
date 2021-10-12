plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("application")

	id("org.jetbrains.dokka")

	id("jacoco")
	id("de.jansauer.printcoverage").version("2.0.0")
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	implementation(project(":api"))
	implementation(project(":database"))

	implementation("io.ktor:ktor-server-core:1.6.1")
	implementation("io.ktor:ktor-server-netty:1.6.1")
	implementation("io.ktor:ktor-html-builder:1.6.1")
	implementation("io.ktor:ktor-serialization:1.6.1")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")
	testImplementation("io.ktor:ktor-server-tests:1.6.1")
	implementation("ch.qos.logback:logback-classic:1.2.4")

	implementation("io.ktor:ktor-auth:1.6.1")
	implementation("io.ktor:ktor-auth-jwt:1.6.1")
	implementation("at.favre.lib:bcrypt:0.9.0")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.1")

	implementation("org.apache.tika:tika-core:2.0.0")
}

application {
	mainClass.set("formulaide.server.MainKt")
}

tasks.named<JavaExec>("run") {
	description = "Runs this project as a JVM application (development mode)"

	jvmArgs = listOf("-Dio.ktor.development=true")
}

jacoco {
	toolVersion = "0.8.7"
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
