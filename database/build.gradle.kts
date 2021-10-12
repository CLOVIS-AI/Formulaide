plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")

	id("org.jetbrains.dokka")

	id("jacoco")
	id("de.jansauer.printcoverage").version("2.0.0")
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	api(project(":api"))

	api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.1")
	testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.5.1")
	implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.2.8")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.2.1")

	implementation("ch.qos.logback:logback-classic:1.2.4")
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
	toolVersion = "0.8.7"
}
