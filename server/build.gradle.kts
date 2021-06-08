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
	testImplementation(ktorServer("tests"))
	implementation(logback("classic"))

	implementation(ktor("auth"))
	implementation(ktor("auth-jwt"))
	implementation(bcrypt())

	implementation(arrow("core"))

	implementation(kotlinxCoroutines("core"))
	testImplementation(kotlinxCoroutines("test"))
}

application {
	mainClass.set("formulaide.server.MainKt")
}

jacoco {
	this.toolVersion = Version.jacoco
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)

	reports {
		xml.isEnabled = true
		csv.isEnabled = false
		html.isEnabled = true
	}
}
