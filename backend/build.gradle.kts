plugins {
	kotlin("jvm")
	application
}

dependencies {
	implementation(projects.core)
	implementation(projects.remoteServer)

	implementation(Ktor.server)
	implementation(Ktor.server.netty)
	implementation(Ktor.plugins.serialization)

	implementation(KotlinX.serialization.core)

	implementation("ch.qos.logback:logback-classic:_")
}

application {
	mainClass.set("opensavvy.formulaide.backend.BackendKt")
}
