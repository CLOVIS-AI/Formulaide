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
	implementation("io.ktor:ktor-server-status-pages:2.0.1")
	implementation("io.ktor:ktor-server-conditional-headers:2.0.1")
	implementation("io.ktor:ktor-server-cors:2.0.1")
	implementation("io.ktor:ktor-server-content-negotiation:2.0.1")
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	implementation(project(":api"))
	implementation(project(":database"))

	implementation(Ktor.server.core)
	implementation(Ktor.server.netty)
	implementation("io.ktor:ktor-server-html-builder:2.0.1")
	implementation("io.ktor:ktor-serialization-kotlinx-json:2.0.1")
	implementation("ch.qos.logback:logback-classic:_")

	implementation("io.ktor:ktor-server-auth:2.0.1")
	implementation("io.ktor:ktor-server-auth-jwt:2.0.1")
	implementation("at.favre.lib:bcrypt:_")

	implementation(KotlinX.coroutines.core)
	testImplementation(KotlinX.coroutines.test)

	implementation("org.apache.tika:tika-core:_")
	testImplementation("io.ktor:ktor-server-tests-jvm:2.0.1")
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
		xml.required.set(true)
		csv.required.set(false)
		html.required.set(true)
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
