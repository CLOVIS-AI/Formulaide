package formulaide.api.data

import formulaide.api.fields.FormField
import formulaide.api.types.OrderedListElement.Companion.checkOrderValidity
import formulaide.api.types.Referencable
import formulaide.api.users.TokenResponse
import kotlinx.serialization.Serializable

/**
 * The declaration of a form.
 *
 * This type represents the structure of a form. For a submission, see [FormSubmission].
 *
 * A form declaration is composed of 3 parts:
 * - Metadata ([name], [public]…)
 * - Fields that the user needs to fill in ([fields])
 * - Actions that will be taken to check that a submission is valid ([actions])
 *
 * @property name The display name of this form
 * @property public Whether this form is visible from anonymous users.
 * If `false`, only users that have logged in (that possess a valid [TokenResponse]) can see it.
 * @property fields The fields that the user needs to fill in when creating a [FormSubmission].
 * @property actions The steps taken to validate a [FormSubmission].
 * If the user querying this form is not allowed to see the list of actions
 * (for example, anonymous users), the list is empty.
 */
@Serializable
data class Form(
	override val id: String,
	val name: String,
	val open: Boolean,
	val public: Boolean,
	val fields: List<FormField>,
	val actions: List<Action>,
) : Referencable {
	init {
		fields.checkOrderValidity()
		require(name.isNotBlank()) { "Le nom d'un formulaire ne peut pas être vide : '$name'" }
	}
}
