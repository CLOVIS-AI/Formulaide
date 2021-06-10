package formulaide.api.data

import formulaide.api.users.ServiceId
import formulaide.api.users.User
import kotlinx.serialization.Serializable

/**
 * ID of [Action].
 */
typealias ActionId = Int

/**
 * An action is a step taken to validate a [form submission][FormSubmission].
 * Only employees have access to actions (see [Form.actions]).
 *
 * @property type The type of this action. It decides what is stored in [data].
 * @property data The data that relates to this action, in JSON format.
 * See [type] and [ActionType] for information on what is stored.
 */
@Serializable
data class Action(
	val id: ActionId,
	val type: ActionType,
	val data: String,
	override val order: Int,
) : OrderedListElement

/**
 * The type of action.
 * See each element for details.
 */
@Serializable
enum class ActionType{
	/**
	 * A service should take a look at this form.
	 *
	 * - [Action.data] is a [ServiceId].
	 */
	SERVICE_REVIEW,

	/**
	 * A specific employee should take a look at this form.
	 *
	 * - [Action.data] is a [User.email].
	 */
	EMPLOYEE_REVIEW,
}
