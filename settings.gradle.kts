rootProject.name = "Formulaide"

pluginManagement {
	repositories {
		gradlePluginPortal()
		maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
	}
}

plugins {
	id("de.fayard.refreshVersions") version "0.51.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
	"backend",

	"core:core-data",
	"core:core-users",
	"core:core-domain",

	"remote-common",
	"remote-client",
	"remote-server",

	"mongo",

	"fake",
	"test",
	"test-structure",
)

buildCache {
	val username = System.getenv("GRADLE_BUILD_CACHE_CREDENTIALS")?.split(':')?.get(0)
	val password = System.getenv("GRADLE_BUILD_CACHE_CREDENTIALS")?.split(':')?.get(1)

	val mainBranch: String? = System.getenv("CI_DEFAULT_BRANCH")
	val currentBranch: String? = System.getenv("CI_COMMIT_REF_NAME")
	val runningForTag = System.getenv().containsKey("CI_COMMIT_TAG")

	remote<HttpBuildCache> {
		url = uri("https://gradle.opensavvy.dev/cache/")

		if (username != null && password != null) credentials {
			this.username = username
			this.password = password
		}

		isPush = (mainBranch != null && currentBranch != null && mainBranch == currentBranch) || runningForTag
	}
}
