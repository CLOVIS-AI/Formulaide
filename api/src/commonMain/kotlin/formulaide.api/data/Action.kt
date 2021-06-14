package formulaide.api.data

import formulaide.api.types.Email
import formulaide.api.users.Service
import formulaide.api.users.ServiceId
import formulaide.api.users.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ID of [Action].
 */
typealias ActionId = Int

/**
 * An action is a step taken to validate a [form submission][FormSubmission].
 * Only employees have access to actions (see [Form.actions]).
 */
@Serializable
sealed class Action : OrderedListElement {

	abstract val id: ActionId

	/**
	 * An employee of a specified [service] must check that the submission is valid.
	 */
	@Serializable
	@SerialName("SERVICE_REVIEW")
	data class ServiceReview(
		override val id: ActionId,
		override val order: Int,
		val service: ServiceId,
	) : Action() {
		constructor(id: ActionId, order: Int, service: Service) : this(id, order, service.id)
	}

	/**
	 * A specific [employee][employee] must check that the submission is valid.
	 */
	@Serializable
	@SerialName("EMPLOYEE_REVIEW")
	data class EmployeeReview(
		override val id: ActionId,
		override val order: Int,
		val employee: Email,
	) : Action() {
		constructor(id: ActionId, order: Int, employee: User) : this(id, order, employee.email)
	}
}
