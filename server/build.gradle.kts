plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("application")

	id("org.jetbrains.dokka")

	id("jacoco")
	id("de.jansauer.printcoverage").version(Version.printCoverage)
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	implementation(project(":api"))
	implementation(project(":database"))

	implementation(ktorServer("core"))
	implementation(ktorServer("netty"))
	implementation(ktor("html-builder"))
	implementation(ktor("serialization"))
	implementation(kotlinxSerialization("json"))
	testImplementation(ktorServer("tests"))
	implementation(logback("classic"))

	implementation(ktor("auth"))
	implementation(ktor("auth-jwt"))
	implementation(bcrypt())

	implementation(kotlinxCoroutines("core"))
	testImplementation(kotlinxCoroutines("test"))

	implementation(apacheTika("core"))
}

application {
	mainClass.set("formulaide.server.MainKt")
}

tasks.named<JavaExec>("run") {
	description = "Runs this project as a JVM application (development mode)"

	jvmArgs = listOf("-Dio.ktor.development=true")
}

jacoco {
	toolVersion = Version.jacocoVersion
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
}

tasks.processResources {
	if (!project.hasProperty("devMode"))
		dependsOn("copyFrontend")
}
