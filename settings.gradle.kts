rootProject.name = "formulaide"

plugins {
	id("de.fayard.refreshVersions") version "0.30.1"
}

include("api")
include("client")
include("server")
include("database")
include("ui")
