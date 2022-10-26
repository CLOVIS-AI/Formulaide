package opensavvy.formulaide.api

import kotlinx.datetime.toInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.formulaide.api.Field.*
import opensavvy.formulaide.core.AbstractTemplateVersions
import opensavvy.formulaide.core.AbstractTemplates
import opensavvy.formulaide.core.InputConstraints
import opensavvy.formulaide.core.Template
import opensavvy.spine.Id
import opensavvy.formulaide.core.Field as CoreField

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

//region Core -> API

fun CoreField.toApi(): Field {
	val imported = importedFrom?.let {
		api2.templates.id.version.idOf(it.template.id, it.version.toString())
	}

	return when (this) {
		is CoreField.Arity -> {
			Arity(
				label,
				child.toApi(),
				allowed.first,
				allowed.last,
				imported,
			)
		}

		is CoreField.Choice -> Choice(
			label,
			indexedFields.mapValues { (_, v) -> v.toApi() },
			imported,
		)

		is CoreField.Group -> Group(
			label,
			indexedFields.mapValues { (_, v) -> v.toApi() },
			imported,
		)

		is CoreField.Input -> Input(
			label,
			imported,
			when (val input = input) {
				is InputConstraints.Boolean -> Input.Constraints.Boolean
				is InputConstraints.Date -> Input.Constraints.Date
				is InputConstraints.Email -> Input.Constraints.Email
				is InputConstraints.Integer -> Input.Constraints.Integer(input.min, input.max)
				is InputConstraints.Phone -> Input.Constraints.Phone
				is InputConstraints.Text -> Input.Constraints.Text(input.maxLength)
				is InputConstraints.Time -> Input.Constraints.Time
			}
		)

		is CoreField.Label -> Label(
			label,
			imported,
		)
	}
}

//endregion
//region API -> Core

fun Field.toCore(templates: AbstractTemplates, templateVersions: AbstractTemplateVersions): CoreField {
	val imported = importedFrom?.let {
		val idSize = it.resource.segments.size
		val templateId = it.resource.segments[idSize - 2].segment
		val versionId = it.resource.segments[idSize - 1].segment

		val template = Template.Ref(templateId, templates)
		Template.Version.Ref(template, versionId.toInstant(), templateVersions)
	}

	return when (this) {
		is Arity -> CoreField.Arity(
			label,
			child.toCore(templates, templateVersions),
			min..max,
			imported,
		)

		is Choice -> CoreField.Choice(
			label,
			children.mapValues { (_, v) -> v.toCore(templates, templateVersions) },
			imported,
		)

		is Group -> CoreField.Group(
			label,
			children.mapValues { (_, v) -> v.toCore(templates, templateVersions) },
			imported,
		)

		is Input -> CoreField.Input(
			label,
			when (val input = input) {
				is Input.Constraints.Boolean -> InputConstraints.Boolean
				is Input.Constraints.Date -> InputConstraints.Date
				is Input.Constraints.Email -> InputConstraints.Email
				is Input.Constraints.Integer -> InputConstraints.Integer(input.min, input.max)
				is Input.Constraints.Phone -> InputConstraints.Phone
				is Input.Constraints.Text -> InputConstraints.Text(input.maxLength)
				is Input.Constraints.Time -> InputConstraints.Time
			},
			imported,
		)

		is Label -> CoreField.Label(
			label,
			imported,
		)
	}
}

//endregion
