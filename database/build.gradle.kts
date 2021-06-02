plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
}

dependencies {
	implementation(kotlin("stdlib"))
	testImplementation(kotlin("test"))
	testImplementation(kotlin("test-junit"))

	api(project(":api"))
}
