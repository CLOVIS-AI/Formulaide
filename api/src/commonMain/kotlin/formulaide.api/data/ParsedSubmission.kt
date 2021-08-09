package formulaide.api.data

import formulaide.api.fields.DeepFormField
import formulaide.api.fields.FormField
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.SimpleField

/**
 * The data contained in a [FormSubmission], parsed into a recursive data structure that can easily be traversed to find some information.
 *
 * This data structure can be obtained from [FormSubmission.parse].
 */
data class ParsedSubmission(
	val fields: List<ParsedField<ShallowFormField>>,
) {

	/**
	 * Generates a string representation of the data contained in this object.
	 */
	fun toDeepString(): String {
		fun toDeepString(field: ParsedField<*>, builder: ArrayList<String>) {
			if (field.rawValue != null)
				builder += "${field.fullKeyString} -> ${field.rawValue}"

			if (field.children != null)
				for (subField in field.children!!)
					toDeepString(subField, builder)
		}

		val builder = ArrayList<String>()
		fields.forEach { toDeepString(it, builder) }
		return builder.joinToString(separator = "\n")
	}
}

/**
 * A [FormField] with its [value].
 *
 * @property key The ID of this field submission (see [FormSubmission] for the explanation of the algorithm)
 * @property parent The parent of this field submission, or `null` if field is the root, or if the field is not initialized
 *
 * @property rawValue The string representation of the submitted value for this field (see [FormSubmission] for the explanation of the values, [value])
 * @property value The Kotlin representation of the submitted value (see [rawValue])
 *
 * @property constraint The [FormField] this submission corresponds to
 * @property children The sub-fields of this submission (eg. union fields, composite fields…)
 */
sealed class ParsedField<out F : FormField>(
	key: String,
) {

	var key = key
		internal set
	var parent: ParsedField<*>? = null
		internal set

	val fullKey: List<String>
		get() = (parent?.fullKey ?: emptyList()) + key
	val fullKeyString: String
		get() = fullKey.joinToString(separator = ":")

	abstract val rawValue: String?
	abstract val value: Any?

	abstract val constraint: F
	abstract val children: List<ParsedField<*>>?
}

data class ParsedUnion<out Union : FormField.Union<Choice>, out Choice : FormField> internal constructor(
	override val constraint: Union,
	override val value: Choice,
	override val children: List<ParsedField<Choice>>,
) : ParsedField<Union>(constraint.id) {
	init {
		require(value in constraint.options) { "L'option choisie ($value) doit faire partie des options données : ${constraint.options}" }

		if (value !is FormField.Simple || value.simple !is SimpleField.Message) {
			require(children.size == 1) { "On ne peut choisir qu'un seul élément d'une union, mais plusieurs choix ont été trouvés : $children" }
		}
	}

	override val rawValue: String get() = value.id
}

data class ParsedComposite<out Composite : FormField.Composite> internal constructor(
	override val constraint: Composite,
	override val children: List<ParsedField<DeepFormField>>,
) : ParsedField<Composite>(constraint.id) {
	override val rawValue: String? get() = null
	override val value: Any? get() = null
}

data class ParsedList<out Field : FormField> internal constructor(
	override val constraint: Field,
	override val children: List<ParsedField<Field>>,
) : ParsedField<Field>(constraint.id) {
	override val rawValue: String? get() = null
	override val value: Any? get() = null
}

data class ParsedSimple<out Simple : FormField.Simple> internal constructor(
	override val constraint: Simple,
	override val rawValue: String?,
) : ParsedField<Simple>(constraint.id) {
	init {
		constraint.simple.parse(rawValue)
	}

	override val value: Any? get() = rawValue
	override val children: List<ParsedField<*>>? get() = null
}
