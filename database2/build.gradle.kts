plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")

	id("org.jetbrains.dokka")

	id("jacoco")
	id("de.jansauer.printcoverage")
}

dependencies {
	api(projects.core2)

	api(KotlinX.coroutines.core)

	implementation("org.litote.kmongo:kmongo-coroutine-serialization:_")

	implementation("opensavvy:logger:_")
	implementation("ch.qos.logback:logback-classic:_")

	implementation("at.favre.lib:bcrypt:_")

	testImplementation(Kotlin.test)
	testImplementation(Kotlin.test.junit)
	testImplementation(KotlinX.coroutines.test)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)

	reports {
		xml.required.set(true)
		csv.required.set(false)
		html.required.set(true)
	}
}

jacoco {
	toolVersion = "0.8.8"
}
