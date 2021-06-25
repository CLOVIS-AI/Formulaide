package formulaide.api.data

import formulaide.api.data.Data.*
import formulaide.api.data.Data.Simple.SimpleDataId
import formulaide.api.types.Arity
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A data type that can appear in a form or in storage.
 *
 * There are 4 groups of data types:
 * - Simple types ([SimpleDataId.TEXT], [SimpleDataId.INTEGER]…), represented by [Simple]
 * - Compound types (created by the administrator, and stored for analysis and processing), represented by [Compound]
 * - Union types (that appear inside compound types or inside forms), represented by [Union]
 * - List types (that appear inside compound types or inside forms), implicit (see [Arity])
 *
 * When serialized, an additional field `type` is added, that corresponds to one of the three first categories.
 */
@Serializable
sealed class Data {
	companion object {

		/**
		 * Factory method to reference a simple data.
		 * @see Data
		 * @see Simple
		 * @see SimpleDataId
		 */
		fun simple(id: SimpleDataId) = Simple(id)

		/**
		 * Factory method to reference a [compound data][CompoundData].
		 * @see Data
		 * @see Compound
		 * @see CompoundData
		 */
		fun compound(compound: CompoundData) = Compound(compound.id)

		/**
		 * Factory method to reference a [compound data][CompoundData].
		 * @see Data
		 * @see Compound
		 * @see CompoundData
		 */
		fun compoundById(id: CompoundDataId) = Compound(id)

		/**
		 * Factory method to reference a recursive [compound data][CompoundData].
		 * @see compound
		 * @see SPECIAL_TOKEN_RECURSION
		 */
		fun recursiveCompound() = Compound(SPECIAL_TOKEN_RECURSION)

		/**
		 * Factory method to reference a [union data][UnionDataField].
		 * @see Data
		 * @see Union
		 * @see UnionDataField
		 */
		fun union(ids: List<UnionDataField>) = Union(ids)
	}

	/**
	 * A reference to a [CompoundData].
	 */
	@Serializable
	@SerialName("COMPOUND")
	data class Compound(
		val id: CompoundDataId
	) : Data()

	/**
	 * A union of multiple different data types.
	 * @see UnionDataField
	 */
	@Serializable
	@SerialName("UNION")
	data class Union(
		val elements: List<UnionDataField>
	) : Data()

	/**
	 * A simple type.
	 * @see SimpleDataId
	 */
	@Serializable
	@SerialName("SIMPLE")
	data class Simple(
		val id: SimpleDataId,
	) : Data() {

		@Serializable
		enum class SimpleDataId(val displayName: String) {

			/**
			 * A simple text field.
			 *
			 * @see MESSAGE
			 */
			TEXT("Texte") {
				override fun validate(value: String?) = value?.ifBlank { null }
			},

			/**
			 * An unmodifiable text field.
			 * This data type is used to display text in the form.
			 *
			 * When using this type, the name of the field ([CompoundDataField.name], [UnionDataField.name])
			 * contains the text that should be displayed to the user.
			 * When [submitting a form][FormSubmission], the expected value is `null` (since there is no meaningful user input).
			 *
			 * @see TEXT
			 */
			MESSAGE("Message") {
				override fun validate(value: String?) = value
			},

			/**
			 * The user should input an integer.
			 *
			 * The type corresponds to Kotlin's [Long] (a 64-bit signed integer).
			 */
			INTEGER("Nombre entier") {
				override fun validate(value: String?) = value?.toLongOrNull()
			},

			/**
			 * The user should input a decimal number.
			 *
			 * The type corresponds to Kotlin's [Double] (a 64-bit double precision floating point number).
			 */
			DECIMAL("Nombre décimal") {
				override fun validate(value: String?) = value?.toDoubleOrNull()
			},

			/**
			 * A simple checkbox.
			 */
			BOOLEAN("Bouton à cocher") {
				override fun validate(value: String?) = value?.toBooleanStrictOrNull()
			},

			;

			/**
			 * Validates that a given [value] corresponds to this [simple type][SimpleDataId].
			 *
			 * Each implementation narrows the return type.
			 */
			abstract fun validate(value: String?): Any?
		}
	}
}
