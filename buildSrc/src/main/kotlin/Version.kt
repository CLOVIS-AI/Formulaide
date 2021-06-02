object Version {
	const val kotlin = "1.5.10"

	const val serialization = "1.2.1"
}

fun kotlinxSerialization(name: String) =
	"org.jetbrains.kotlinx:kotlinx-serialization-$name:${Version.serialization}"
