rootProject.name = "Formulaide"

pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
	}
}

plugins {
	id("de.fayard.refreshVersions") version "0.40.1"
}

include("api")
include("client")
include("server")
include("database")
include("ui")
include("ui2")
