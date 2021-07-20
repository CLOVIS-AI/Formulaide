@file:Suppress("MemberVisibilityCanBePrivate")

import Version.bcrypt
import Version.kmongo
import Version.ktor
import Version.logback

object Version {
	const val kotlinVersion = "1.5.21"

	const val dokka = "1.4.32"

	const val serialization = "1.2.1"
	const val coroutines = "1.5.1"

	const val jacocoVersion = "0.8.7"
	const val printCoverage = "2.0.0"

	const val react = "17.0.2"
	const val kotlinReact = "$react-pre.219-kotlin-$kotlinVersion"

	const val postcss = "8.3.5"
	const val postcssLoader = "6.1.0"
	const val autoprefixer = "10.2.6"
	const val tailwind = "2.2.4"

	const val ktor = "1.6.0"
	const val logback = "1.2.3"
	const val bcrypt = "0.9.0"

	const val kmongo = "4.2.8"
}

fun kotlinxSerialization(name: String) =
	"org.jetbrains.kotlinx:kotlinx-serialization-$name:${Version.serialization}"

fun kotlinxCoroutines(name: String) =
	"org.jetbrains.kotlinx:kotlinx-coroutines-$name:${Version.coroutines}"

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

fun kmongo(name: String) =
	"org.litote.kmongo:$name:$kmongo"
