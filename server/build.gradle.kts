plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("application")
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	implementation(project(":api"))
}

application {
	mainClass.set("formulaide.server.MainKt")
}
