package formulaide.api.users

import formulaide.api.data.Form
import formulaide.api.data.RecordState
import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.types.Referencable
import formulaide.api.types.ReferenceId
import kotlinx.serialization.Serializable

/**
 * Represents a user.
 *
 * @property enabled `true` if it is currently allowed to login as this user.
 * @see NewUser
 * @see PasswordLogin
 */
@Serializable
data class User(
	val email: Email,
	val fullName: String,
	val services: Set<Ref<Service>>,
	val administrator: Boolean,
	val enabled: Boolean = true,
) : Referencable {

	override val id: ReferenceId
		get() = email.email

	companion object {
		// Yes, this is a copy-paste of the code in the 'core' module
		// It will be removed when the Spine migration is complete
		val User?.role
			get() = when {
				this == null -> formulaide.core.User.Role.ANONYMOUS
				!administrator -> formulaide.core.User.Role.EMPLOYEE
				administrator -> formulaide.core.User.Role.ADMINISTRATOR
				else -> error("Should never happen")
			}
	}
}

/**
 * Is this user allowed to access this [form]'s [state]?
 *
 * If [state] is `null`, it is interpreted as asking whether the user is allowed to access the "all records" page for the [form].
 */
fun User.canAccess(form: Form, state: RecordState?): Boolean {
	val user = this

	if (user.administrator)
		return true

	return when (state) {
		is RecordState.Action -> {
			user.services.any { service ->
				service.id == state.current.apply {
					loadFrom(
						form.actions,
						lazy = true
					)
				}.obj.reviewer.id
			}
		}

		is RecordState.Refused, null -> {
			user.services.any { service -> service.id in form.actions.map { it.reviewer.id } }
		}
	}
}
