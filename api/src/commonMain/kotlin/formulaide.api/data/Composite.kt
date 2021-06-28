package formulaide.api.data

import formulaide.api.fields.DataField
import formulaide.api.types.OrderedListElement.Companion.checkOrderValidity
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

		val ids = fields.distinctBy { it.id }
		require(ids == fields.toList()) { "L'identité d'un champ ne doit pas apparaitre plusieurs fois dans une même donnée" }
	}

	fun validate(composites: List<Composite>) {
		require(fields.isNotEmpty()) { "Il est interdit de créer une donnée vide" }
		fields.forEach { it.validate(composites) }
	}
}

/**
 * Special ID that can be used for fields in data to refer to their parent data even before it was created (therefore doesn't have an ID yet).
 */
// The semicolon is not allowed in normal IDs
const val SPECIAL_TOKEN_RECURSION: ReferenceId = "special:myself"
