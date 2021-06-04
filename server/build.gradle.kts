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
	implementation(logback("classic"))
}

application {
	mainClass.set("formulaide.server.MainKt")
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)

	reports {
		xml.isEnabled = true
		csv.isEnabled = false
		html.isEnabled = true
	}
}
