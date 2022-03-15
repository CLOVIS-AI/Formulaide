rootProject.name = "Formulaide"

plugins {
	id("de.fayard.refreshVersions") version "0.40.1"
}

include("api")
include("client")
include("server")
include("database")
include("ui")
