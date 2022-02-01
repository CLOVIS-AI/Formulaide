rootProject.name = "formulaide"

plugins {
	id("de.fayard.refreshVersions") version "0.40.0"
}

include("api")
include("client")
include("server")
include("database")
include("ui")
