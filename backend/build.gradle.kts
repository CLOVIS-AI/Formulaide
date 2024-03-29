plugins {
	kotlin("jvm")
	application
}

dependencies {
	implementation(projects.core.coreData)
	implementation(projects.core.coreUsers)
	implementation(projects.core.coreDomain)
	implementation(projects.fake)
	implementation(projects.mongo)
	implementation(projects.remoteServer)

	implementation("opensavvy.pedestal:logger:_")

	implementation(Ktor.server)
	implementation(Ktor.server.netty)
	implementation(Ktor.server.callLogging)
	implementation(Ktor.server.statusPages)
	implementation(Ktor.server.hsts)
	implementation(Ktor.plugins.serialization)

	implementation(KotlinX.serialization.core)

	implementation("ch.qos.logback:logback-classic:_")
	implementation("io.ktor:ktor-server-call-logging-jvm:2.2.2")
}

application {
	mainClass.set("opensavvy.formulaide.backend.BackendKt")

	if (project.hasProperty("developmentMode"))
		applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

kotlin {
	jvmToolchain(17)
}

tasks.test {
	useJUnitPlatform()
}
