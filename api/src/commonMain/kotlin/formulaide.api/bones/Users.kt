package formulaide.api.bones

import formulaide.api.data.Form
import formulaide.api.data.RecordState
import kotlinx.serialization.Serializable

@Serializable
data class ApiUser(
	val email: String,
	val fullName: String,
	val departments: Set<Int>,
	val administrator: Boolean,
	val enabled: Boolean,
) {

	/**
	 * Is this user allowed to access this [form]'s [state]?
	 *
	 * If [state] is `null`, it is interpreted as asking whether the user is allowed to access the "all records" page for the [form].
	 */
	// In the future, this method will be moved to the 'core' module
	fun canAccess(form: Form, state: RecordState?): Boolean {
		val user = this

		if (user.administrator)
			return true

		return when (state) {
			is RecordState.Action -> {
				user.departments.any { department ->
					department.toString() == state.current.apply {
						loadFrom(
							form.actions,
							lazy = true
						)
					}.obj.reviewer.id
				}
			}

			is RecordState.Refused, null -> {
				user.departments.any { service -> service.toString() in form.actions.map { it.reviewer.id } }
			}
		}
	}

}

@Serializable
data class ApiPasswordLogin(
	val email: String,
	val password: String,
)

@Serializable
data class ApiNewUser(
	val email: String,
	val fullName: String,
	val departments: Set<Int>,
	val administrator: Boolean,
	val password: String,
)

@Serializable
data class ApiUserEdition(
	val enabled: Boolean? = null,
	val administrator: Boolean? = null,
	val departments: Set<Int>? = null,
)

@Serializable
data class ApiUserPasswordEdition(
	val oldPassword: String?,
	val newPassword: String,
)
