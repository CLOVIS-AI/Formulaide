plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")

	id("org.jetbrains.dokka")

	id("jacoco")
	id("de.jansauer.printcoverage")
}

dependencies {
	implementation("io.ktor:ktor-server-status-pages:_")
	implementation("io.ktor:ktor-server-conditional-headers:_")
	implementation("io.ktor:ktor-server-cors:_")
	implementation("io.ktor:ktor-server-content-negotiation:_")
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	implementation(projects.api2.common)
	implementation(projects.database2)

	implementation("opensavvy:spine-ktor-server:_")

	implementation(Ktor.server.core)
	implementation(Ktor.server.netty)
	implementation("io.ktor:ktor-server-html-builder:_")
	implementation("io.ktor:ktor-serialization-kotlinx-json:_")
	implementation("ch.qos.logback:logback-classic:_")
	implementation("io.ktor:ktor-server-call-logging:_")

	implementation(KotlinX.coroutines.core)
	testImplementation(KotlinX.coroutines.test)

	implementation("org.apache.tika:tika-core:_")
	testImplementation("io.ktor:ktor-server-tests-jvm:_")
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
