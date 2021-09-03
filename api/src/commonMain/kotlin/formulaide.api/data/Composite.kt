package formulaide.api.data

import formulaide.api.fields.DataField
import formulaide.api.fields.asSequence
import formulaide.api.fields.load
import formulaide.api.types.OrderedListElement.Companion.checkOrderValidity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.ids
import formulaide.api.types.Referencable
import formulaide.api.types.ReferenceId
import kotlinx.serialization.Serializable

/**
 * A data type built from the composition of other data types (record type, product type).
 *
 * Through [DataField], a composite data can include unions, other composite datas, or even be recursive.
 *
 * @property name The display name of this data type.
 * @property fields The different types that are part of this compound.
 * The list cannot be empty.
 *
 * @see formulaide.api.dsl.composite
 */
@Serializable
data class Composite(
	override val id: ReferenceId,
	val name: String,
	val fields: List<DataField>,
) : Referencable {
	init {
		require(name.isNotBlank()) { "Le nom d'une donnée ne peut pas être vide : '$name'" }
		fields.checkOrderValidity()

		require(fields.ids() == fields.ids()
			.distinct()) { "Plusieurs champs ont le même identifiant : ${fields.ids()}" }
	}

	/**
	 * Loads this [Composite], including all of its fields.
	 *
	 * Parameters [allowNotFound] and [lazy] are passed to [Ref.loadFrom].
	 * @see loadAllowRecursive
	 */
	fun load(composites: List<Composite>, allowNotFound: Boolean = false, lazy: Boolean = true) =
		fieldsRecursively.forEach { it.load(composites, allowNotFound, lazy) }

	/**
	 * Loads this [Composite], but allows a field to refer to the current composite using the [SPECIAL_TOKEN_RECURSION].
	 * This token is only valid for composite data that are being created and that do not yet have an [id].
	 *
	 * Parameters [allowNotFound] and [lazy] are passed to [Ref.loadFrom].
	 * @see load
	 */
	fun loadAllowRecursive(
		composites: List<Composite>,
		allowNotFound: Boolean = false,
		lazy: Boolean = true,
	) =
		load(composites + Composite(SPECIAL_TOKEN_RECURSION, "Myself (recursive)", emptyList()),
		     allowNotFound,
		     lazy)

	/**
	 * Checks that this [Composite] respects all its constraints.
	 *
	 * This composite must have been [loaded][load] before this call.
	 */
	fun validate() {
		require(fields.isNotEmpty()) { "Il est interdit de créer une donnée vide" } // not in the constructor, because self-recursive composites do not have fields
		fieldsRecursively.forEach { it.validate() }
	}
}

/**
 * A [Sequence] of all [DataField]s in this [Composite].
 *
 * Unlike [Composite.fields], this sequence is recursive, and can be used as an approximation of a Monad over [Composite].
 */
val Composite.fieldsRecursively: Sequence<DataField>
	get() = fields.asSequence().flatMap { it.asSequence() }

/**
 * Special ID that can be used for fields in data to refer to their parent data even before it was created (therefore doesn't have an ID yet).
 */
// The semicolon is not allowed in normal IDs
const val SPECIAL_TOKEN_RECURSION: ReferenceId = "special:myself"
