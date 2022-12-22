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
