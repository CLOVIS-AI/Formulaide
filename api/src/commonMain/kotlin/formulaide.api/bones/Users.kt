package formulaide.api.bones

import formulaide.api.data.Form
import formulaide.api.data.RecordState
import formulaide.core.Department
import formulaide.core.Ref
import formulaide.core.User
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ApiUser(
	val email: String,
	val fullName: String,
	val departments: Set<@Contextual Ref<Department>>,
	val administrator: Boolean,
	val enabled: Boolean,
)

fun User.canAccess(form: Form, state: RecordState?): Boolean {
	if (administrator)
		return true

	return when (state) {
		is RecordState.Action -> {
			departments.any { department ->
				department.toString() == state.current.apply {
					loadFrom(
						form.actions,
						lazy = true
					)
				}.obj.reviewer.id
			}
		}

		is RecordState.Refused, null -> {
			departments.any { service -> service.toString() in form.actions.map { it.reviewer.id } }
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
	val departments: Set<@Contextual Ref<Department>>,
	val administrator: Boolean,
	val password: String,
)

@Serializable
data class ApiUserEdition(
	val enabled: Boolean? = null,
	val administrator: Boolean? = null,
	val departments: Set<@Contextual Ref<Department>>? = null,
)

@Serializable
data class ApiUserPasswordEdition(
	val oldPassword: String?,
	val newPassword: String,
)
