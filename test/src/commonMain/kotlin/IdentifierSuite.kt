package opensavvy.formulaide.test

import io.kotest.assertions.fail
import io.kotest.matchers.shouldBe
import opensavvy.formulaide.core.utils.IdentifierParser
import opensavvy.formulaide.core.utils.IdentifierWriter
import opensavvy.formulaide.test.structure.*

private fun <T> Suite.identifierParsingSuite(
	createParser: Setup<IdentifierParser<T>>,
	writers: Iterator<Pair<String, SetupProvider<IdentifierWriter>>>,
) = suite("Check the identifier") {
	var atLeastOne = false

	for ((name, writerProvider) in writers) {
		atLeastOne = true

		test("Converting $name to an identifier then parsing it should return itself") {
			val writer by writerProvider

			val initial = prepare(writer)
			val parser = prepare(createParser)
			println("""
				Writer: $initial
				Parser: $parser
			""".trimIndent())

			val identifier = initial.toIdentifier()
			val parsed = parser.fromIdentifier(identifier)

			parsed shouldBe initial
		}
	}

	if (!atLeastOne) {
		test("At least one writer has been provided to the identifierParsingSuite function") {
			fail("At least one writer should have been provided.")
		}
	}
}

fun <T, I> Suite.identifierParsingSuite(
	createParser: Setup<IdentifierParser<T>>,
	vararg values: I,
	builder: suspend TestScope.(I) -> IdentifierWriter,
) {
	identifierParsingSuite(
		createParser,
		values.map { it.toString() to prepared { builder(it) } }.iterator(),
	)
}
