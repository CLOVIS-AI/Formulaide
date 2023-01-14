plugins {
	kotlin("jvm")
	application
}

dependencies {
	implementation(projects.core)
	implementation(projects.remoteServer)

	implementation(Ktor.server)
	implementation(Ktor.plugins.serialization)

	implementation(KotlinX.serialization.core)
}

application {
	mainClass.set("opensavvy.formulaide.backend.BackendKt")
}
