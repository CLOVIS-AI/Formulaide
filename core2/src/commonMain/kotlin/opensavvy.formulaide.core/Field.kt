package opensavvy.formulaide.core

import opensavvy.formulaide.core.Field.*

/**
 * A field in a form or a template.
 *
 * Fields are recursive data structures, different types exist:
 * - [labels][Label] are simple text displayed to the user,
 * - [inputs][Input] are regular inputs which request some information from the user,
 * - [choices][Choice] allow the user to select and answer a single option,
 * - [groups][Group] join multiple fields into a single cohesive unit,
 * - [lists][Arity] allow to mark fields as optional, or to allow answering a single field multiple times.
 */
sealed class Field {

	/**
	 * Short explanation of the role of this field, displayed to the user.
	 *
	 * Should not be blank.
	 */
	abstract val label: String

	/**
	 * Children of this field.
	 *
	 * Fields should be displayed to the user in the same order as they appear in this dictionary (not necessarily the order of the keys).
	 */
	abstract val indexedFields: Map<Int, Field>

	/**
	 * Children of this field.
	 *
	 * Fields should be displayed to the user in the same order as they appear in this dictionary (not necessarily the order of the keys).
	 */
	val fields: Sequence<Field>
		get() = indexedFields
			.values
			.asSequence()

	/**
	 * Origin of this field.
	 *
	 * If this field was imported from a [Template], this stores the version of that template.
	 *
	 * If this field was not imported from a template, it is `null`.
	 */
	abstract val importedFrom: Template.Version.Ref?

	protected fun verify() {
		require(label.isNotBlank()) { "Le libellé d'un champ ne peut pas être vide : '$label'" }
	}

	//region Types

	/**
	 * A field with no input.
	 *
	 * Visually, the [label] is displayed but no input is requested of the user.
	 *
	 * This type is used to embed non-interactive information into a form (legal text…).
	 *
	 * Labels never have children.
	 */
	data class Label(
		override val label: String,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {
		override val indexedFields: Map<Int, Field>
			get() = emptyMap()

		init {
			verify()
		}

		override fun toString() = "Label($label)"
	}

	/**
	 * A regular input field.
	 *
	 * This field doesn't have children, but it does directly request some [input] from the user.
	 */
	data class Input(
		override val label: String,
		val input: InputConstraints,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {
		override val indexedFields: Map<Int, Field>
			get() = emptyMap()

		init {
			verify()
		}

		override fun toString() = "Input($label, $input)"
	}

	/**
	 * Multiple choices among which the user must select one.
	 */
	data class Choice(
		override val label: String,
		override val indexedFields: Map<Int, Field>,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {

		init {
			verify()
		}

		override fun toString() = "Choice($label, $indexedFields)"
	}

	/**
	 * Multiple fields that the user must fill in.
	 */
	data class Group(
		override val label: String,
		override val indexedFields: Map<Int, Field>,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {

		init {
			verify()
		}

		override fun toString() = "Group($label, $indexedFields)"
	}

	/**
	 * Controls whether fields are mandatory or optional.
	 */
	data class Arity(
		override val label: String,
		val child: Field,
		val allowed: UIntRange,
		override val importedFrom: Template.Version.Ref?,
	) : Field() {

		init {
			verify()

			// Answering the same field more than 100 times is probably a mistake
			require(allowed.last < 100u) { "il n'est pas possible de répondre à un même champ plus de 100 fois, vous avez demandé ${allowed.last}" }
		}

		override val indexedFields: Map<Int, Field>
			get() = List(allowed.last.toInt()) { it to child }.toMap()

		override fun toString() = "Arity($label, allowed=$allowed, $child)"
	}

	//endregion

}
