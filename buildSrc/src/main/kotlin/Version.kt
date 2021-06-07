@file:Suppress("MemberVisibilityCanBePrivate")

import Version.bcrypt
import Version.ktor
import Version.logback

object Version {
	const val kotlinVersion = "1.5.10"

	const val dokka = "1.4.32"

	const val serialization = "1.2.1"

	const val jacoco = "0.8.7"
	const val printCoverage = "2.0.0"

	const val react = "17.0.2"
	const val kotlinReact = "$react-pre.206-kotlin-$kotlinVersion"

	const val ktor = "1.6.0"
	const val logback = "1.2.3"
	const val bcrypt = "0.9.0"
}

fun kotlinxSerialization(name: String) =
	"org.jetbrains.kotlinx:kotlinx-serialization-$name:${Version.serialization}"

fun kotlinWrapper(name: String, version: String) =
	"org.jetbrains.kotlin-wrappers:kotlin-$name:$version"

fun ktor(name: String) =
	"io.ktor:ktor-$name:$ktor"

fun ktorServer(name: String) = ktor("server-$name")

fun ktorClient(name: String) = ktor("client-$name")

fun logback(name: String) =
	"ch.qos.logback:logback-$name:$logback"

fun bcrypt() =
	"at.favre.lib:bcrypt:$bcrypt"
