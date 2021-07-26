plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")

	id("org.jetbrains.dokka")

	id("jacoco")
	id("de.jansauer.printcoverage").version(Version.printCoverage)
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	api(project(":api"))

	api(kotlinxCoroutines("core"))
	testImplementation(kotlinxCoroutines("test"))
	implementation(kmongo("kmongo-coroutine-serialization"))

	implementation(kotlinxSerialization("json"))

	implementation(logback("classic"))
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)

	reports {
		xml.isEnabled = true
		csv.isEnabled = false
		html.isEnabled = true
	}
}

jacoco {
	toolVersion = Version.jacocoVersion
}
