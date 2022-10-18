rootProject.name = "Formulaide"

pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
	}
}

plugins {
	id("de.fayard.refreshVersions") version "0.50.2"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Formulaide 1.0
include(
	"core",
	"api",
	"client",
	"server",
	"database",
	"ui",
)

// Formulaide 2.0
include(
	"core2",
	"api2",
	"api2:common",
	"api2:client",
	"api2:server",
	"server2",
	"database2",
	"ui2",
)
