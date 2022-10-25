package opensavvy.formulaide.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.formulaide.api.Field.*
import opensavvy.spine.Id

/**
 * Fields in a form.
 *
 * Fields a recursive data structures.
 * The different types of fields are:
 * - [Label]: a simple label without any requested data (used for legal text),
 * - [Input]: some information requested of the user,
 * - [Choice]: a choice between multiple options,
 * - [Group]: multiple fields that must all be filled in,
 * - [Arity]: a control for optional or multiple-answer fields.
 */
@Serializable
sealed class Field {

	/**
	 * The explanation for this field, displayed to the user.
	 */
	abstract val label: String

	/**
	 * Subfields.
	 *
	 * If a field has no subfields, this dictionary is empty.
	 *
	 * No particular information must be extracted from the subfield identifier.
	 * Identifiers may or may not be sequential.
	 * The only guaranteed information is that they are unique for a given parent field.
	 */
	abstract val children: Map<Int, Field>

	/**
	 * If this field is imported from a template, this property stores its ID.
	 * The identifier is only stored for the field matching the root of the template.
	 *
	 * If this field is not imported from a template, this property is `null`.
	 */
	abstract val importedFrom: Id?

	@Serializable
	@SerialName("Label")
	class Label(
		override val label: String,
		override val importedFrom: Id?,
	) : Field() {
		override val children: Map<Int, Field> get() = emptyMap()
	}

	@Serializable
	@SerialName("Input")
	class Input(
		override val label: String,
		override val importedFrom: Id?,
		val input: Constraints,
	) : Field() {
		override val children: Map<Int, Field> get() = emptyMap()

		@Serializable
		sealed class Constraints {
			@Serializable
			@SerialName("Text")
			class Text(val maxLength: UInt? = null) : Constraints()

			@Serializable
			@SerialName("Integer")
			class Integer(val min: Long? = null, val max: Long? = null) : Constraints()

			@Serializable
			@SerialName("Boolean")
			object Boolean : Constraints()

			@Serializable
			@SerialName("Email")
			object Email : Constraints()

			@Serializable
			@SerialName("Phone")
			object Phone : Constraints()

			@Serializable
			@SerialName("Date")
			object Date : Constraints()

			@Serializable
			@SerialName("Time")
			object Time : Constraints()
		}
	}

	@Serializable
	@SerialName("Choice")
	class Choice(
		override val label: String,
		override val children: Map<Int, Field>,
		override val importedFrom: Id?,
	) : Field()

	@Serializable
	@SerialName("Group")
	class Group(
		override val label: String,
		override val children: Map<Int, Field>,
		override val importedFrom: Id?,
	) : Field()

	@Serializable
	@SerialName("Arity")
	class Arity(
		override val label: String,
		val child: Field,
		val min: UInt,
		val max: UInt,
		override val importedFrom: Id?,
	) : Field() {
		override val children: Map<Int, Field> get() = List(max.toInt()) { it to child }.toMap()
	}
}
