package opensavvy.formulaide.database.document

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.InputConstraints
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.Field as CoreField

@Serializable
internal sealed class Field {
	abstract val label: String
	abstract val children: Map<Int, Field>
	abstract val fromTemplate: String?
	abstract val fromVersion: Instant?

	@Serializable
	class Label(
		override val label: String,
		override val fromTemplate: String?,
		override val fromVersion: Instant?,
	) : Field() {
		override val children: Map<Int, Field> get() = emptyMap()
	}

	@Serializable
	class Input(
		override val label: String,
		override val fromTemplate: String?,
		override val fromVersion: Instant?,
		val input: Constraints,
	) : Field() {

		override val children: Map<Int, Field> get() = emptyMap()

		@Serializable
		sealed class Constraints {
			@Serializable
			class Text(val maxLength: UInt? = null) : Constraints()

			@Serializable
			class Integer(val min: Long? = null, val max: Long? = null) : Constraints()

			@Serializable
			object Boolean : Constraints()

			@Serializable
			object Email : Constraints()

			@Serializable
			object Phone : Constraints()

			@Serializable
			object Date : Constraints()

			@Serializable
			object Time : Constraints()
		}
	}

	@Serializable
	class Choice(
		override val label: String,
		override val children: Map<Int, Field>,
		override val fromTemplate: String?,
		override val fromVersion: Instant?,
	) : Field()

	@Serializable
	class Group(
		override val label: String,
		override val children: Map<Int, Field>,
		override val fromTemplate: String?,
		override val fromVersion: Instant?,
	) : Field()

	@Serializable
	class Arity(
		override val label: String,
		val child: Field,
		val min: UInt,
		val max: UInt,
		override val fromTemplate: String?,
		override val fromVersion: Instant?,
	) : Field() {
		override val children: Map<Int, Field> get() = List(max.toInt()) { it to child }.toMap()
	}
}

//region Core -> Db

internal fun CoreField.toDb(): Field {
	val children = indexedFields.mapValues { (_, value) -> value.toDb() }
	val fromTemplate = importedFrom?.template?.id
	val fromVersion = importedFrom?.version

	return when (this) {
		is CoreField.Arity -> Field.Arity(
			label,
			child.toDb(),
			allowed.first,
			allowed.last,
			fromTemplate,
			fromVersion,
		)

		is CoreField.Choice -> Field.Choice(
			label,
			children,
			fromTemplate,
			fromVersion,
		)

		is CoreField.Group -> Field.Group(
			label,
			children,
			fromTemplate,
			fromVersion,
		)

		is CoreField.Input -> Field.Input(
			label,
			fromTemplate,
			fromVersion,
			when (val input = input) {
				is InputConstraints.Boolean -> Field.Input.Constraints.Boolean
				is InputConstraints.Date -> Field.Input.Constraints.Date
				is InputConstraints.Email -> Field.Input.Constraints.Email
				is InputConstraints.Integer -> Field.Input.Constraints.Integer(input.min, input.max)
				is InputConstraints.Phone -> Field.Input.Constraints.Phone
				is InputConstraints.Text -> Field.Input.Constraints.Text(input.maxLength)
				is InputConstraints.Time -> Field.Input.Constraints.Time
			}
		)

		is CoreField.Label -> Field.Label(
			label,
			fromTemplate,
			fromVersion
		)
	}
}

//endregion
//region Db -> Core

internal fun Field.toCore(templates: Templates, templateVersions: Templates.Versions): CoreField {
	val indexed = children.mapValues { (_, value) -> value.toCore(templates, templateVersions) }
	val importedFrom = if (fromTemplate != null && fromVersion != null) {
		Template.Version.Ref(Template.Ref(fromTemplate!!, templates), fromVersion!!, templateVersions)
	} else null

	return when (this) {
		is Field.Arity -> CoreField.Arity(
			label,
			child.toCore(templates, templateVersions),
			min..max,
			importedFrom
		)

		is Field.Choice -> CoreField.Choice(
			label,
			indexed,
			importedFrom,
		)

		is Field.Group -> CoreField.Group(
			label,
			indexed,
			importedFrom,
		)

		is Field.Input -> CoreField.Input(
			label,
			when (input) {
				is Field.Input.Constraints.Boolean -> InputConstraints.Boolean
				is Field.Input.Constraints.Date -> InputConstraints.Date
				is Field.Input.Constraints.Email -> InputConstraints.Email
				is Field.Input.Constraints.Integer -> InputConstraints.Integer(input.min, input.max)
				is Field.Input.Constraints.Phone -> InputConstraints.Phone
				is Field.Input.Constraints.Text -> InputConstraints.Text(input.maxLength)
				is Field.Input.Constraints.Time -> InputConstraints.Time
			},
			importedFrom
		)

		is Field.Label -> CoreField.Label(
			label,
			importedFrom,
		)
	}
}

//endregion
