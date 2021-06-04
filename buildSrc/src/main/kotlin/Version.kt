@file:Suppress("MemberVisibilityCanBePrivate")

import Version.ktor
import Version.logback

object Version {
	const val kotlinVersion = "1.5.10"

	const val dokka = "1.4.32"

	const val serialization = "1.2.1"

	const val printCoverage = "2.0.0"

	const val react = "17.0.2"
	const val kotlinReact = "$react-pre.206-kotlin-$kotlinVersion"

	const val ktor = "1.6.0"
	const val logback = "1.2.3"
}

fun kotlinxSerialization(name: String) =
	"org.jetbrains.kotlinx:kotlinx-serialization-$name:${Version.serialization}"

fun kotlinWrapper(name: String, version: String) =
	"org.jetbrains.kotlin-wrappers:kotlin-$name:$version"

fun ktorServer(name: String) =
	"io.ktor:ktor-server-$name:$ktor"

fun ktorClient(name: String) =
	"io.ktor:ktor-client-$name:$ktor"

fun logback(name: String) =
	"ch.qos.logback:logback-$name:$logback"
