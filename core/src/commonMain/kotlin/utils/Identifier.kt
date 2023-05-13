package opensavvy.formulaide.core.utils

import kotlin.jvm.JvmInline

@JvmInline
value class Identifier(
	val text: String,
) {

	init {
		text.forEach(::parseChar)
	}

	private fun parseChar(char: Char) {
		require(char.isLetterOrDigit() || char == ':' || char == '-') { "The character '$char' is not a valid character in an identifier. Found: '$text'" }
	}
}

interface IdentifierWriter {

	fun toIdentifier(): Identifier

}

interface IdentifierParser<out T> {

	fun fromIdentifier(identifier: Identifier): T

	fun fromIdentifier(identifier: String) = fromIdentifier(Identifier(identifier))

}
