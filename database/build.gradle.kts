plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")

	id("org.jetbrains.dokka")

	id("jacoco")
	id("de.jansauer.printcoverage")
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	api(project(":api"))

	api(KotlinX.coroutines.core)
	testImplementation(KotlinX.coroutines.test)
	implementation("org.litote.kmongo:kmongo-coroutine-serialization:_")

	implementation(KotlinX.serialization.json)

	implementation("ch.qos.logback:logback-classic:_")
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
