@file:Suppress("MemberVisibilityCanBePrivate")

object Version {
	const val kotlinVersion = "1.5.10"

	const val serialization = "1.2.1"

	const val printCoverage = "2.0.0"

	const val react = "17.0.2"
	const val kotlinReact = "$react-pre.206-kotlin-$kotlinVersion"
}

fun kotlinxSerialization(name: String) =
	"org.jetbrains.kotlinx:kotlinx-serialization-$name:${Version.serialization}"

fun kotlinWrapper(name: String, version: String) =
	"org.jetbrains.kotlin-wrappers:kotlin-$name:$version"
