plugins {
	kotlin("jvm")
	application
}

dependencies {
	implementation(projects.core)
	implementation(projects.fake)
	implementation(projects.remoteServer)

	implementation(Ktor.server)
	implementation(Ktor.server.netty)
	implementation(Ktor.server.callLogging)
	implementation(Ktor.server.auth)
	implementation(Ktor.plugins.serialization)

	implementation(KotlinX.serialization.core)

	implementation("ch.qos.logback:logback-classic:_")
	implementation("io.ktor:ktor-server-call-logging-jvm:2.2.2")
}

application {
	mainClass.set("opensavvy.formulaide.backend.BackendKt")
	applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}
