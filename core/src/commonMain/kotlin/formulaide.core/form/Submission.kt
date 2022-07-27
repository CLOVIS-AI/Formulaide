package formulaide.core.form

import formulaide.core.field.Field
import formulaide.core.field.FlatField
import formulaide.core.field.resolve
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Backbone
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.requestValue

/**
 * A user's submission to a [field container][Field.Container].
 */
@Serializable
data class Submission(
	@SerialName("_id") val id: String,
	val fields: @Contextual FlatField.Container.Ref,
	val data: Map<Field.Id, String>,
) {

	//region Validation

	suspend fun verify() {
		checkField(Field.Id.idOf(), fields.requestValue().resolve().root, mandatory = true)
	}

	private fun checkField(id: Field.Id, field: Field, mandatory: Boolean) {
		when (field) {
			// Labels don't expect any updates
			is Field.Label -> {}

			// ...id: "value"
			is Field.Input -> {
				val answer = data[id]

				if (mandatory)
					requireNotNull(answer) { "Saisie manquante pour le champ obligatoire '${field.label}' ($id)" }

				if (answer != null)
					field.input.parse(answer)
			}

			// Five options, the user selected the 3rd
			// ...id:   2
			// ...id:2  <answer>
			is Field.Choice -> {
				val answer = data[id]

				if (mandatory)
					requireNotNull(answer) { "Choix manquant pour le champ '${field.label}' ($id)" }

				if (answer != null) {
					val choice = answer.toIntOrNull()
						?: throw IllegalArgumentException("Le choix pour le champ '${field.label}' ($id) n'est pas un identifiant de champ valide : '$answer'")
					val selected = field.child(choice)
						?: throw IllegalArgumentException("Le choix pour le champ '${field.label}' ($id) ne correspond à aucune option disponible : '$choice' n'est pas inclus dans '${field.indexedFields.keys}'")

					checkField(id + choice, selected, mandatory = mandatory)
				}
			}

			// ...id:
			// ...id:0:  <answer to subfield 0>
			// ...id:1:  <answer to subfield 1>
			// ...id:2:  <answer to subfield 2>
			is Field.Group -> {
				for ((subId, subField) in field.indexedFields) {
					checkField(id + subId, subField, mandatory = mandatory)
				}
			}

			// Example: list(2, 3)
			// ...id:
			// ...id:0  <answer #0, mandatory if the list is mandatory>
			// ...id:1  <answer #1, mandatory if the list is mandatory>
			// ...id:2  <answer #2, always optional>
			is Field.List -> {
				for (i in 0..field.allowed.last.toInt()) {
					checkField(id + i, field.field, mandatory = mandatory && i < field.allowed.first.toInt())
				}
			}
		}
	}

	//endregion

	data class Ref(val id: String, override val backbone: SubmissionBackbone) : opensavvy.backbone.Ref<Submission> {
		override fun toString() = "Submission $id"
	}
}

interface SubmissionBackbone : Backbone<Submission> {
	/**
	 * Creates a new submission.
	 */
	suspend fun create(
		form: Ref<Form>,
		versionId: String,
		stepId: String?,
		data: Map<Field.Id, String>,
	): Submission.Ref
}
