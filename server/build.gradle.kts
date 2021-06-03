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
	implementation(project(":database"))
}

application {
	mainClass.set("formulaide.server.MainKt")
}
