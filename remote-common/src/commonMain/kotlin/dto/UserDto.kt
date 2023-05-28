package opensavvy.formulaide.remote.dto

import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.User
import opensavvy.spine.Id
import opensavvy.spine.Parameters

/**
 * DTO for [User].
 */
@Serializable
data class UserDto(
	val email: String,
	val name: String,
	val active: Boolean,
	val administrator: Boolean,
	val departments: Set<Id>,
	val singleUsePassword: Boolean,
) {

	class ListParams : Parameters() {
		var includeClosed by parameter("includeClosed", default = false)
	}

	@Serializable
	object ListFailures

	@Serializable
	class New(
		val email: String,
		val name: String,
		val administrator: Boolean = false,
	)

	@Serializable
	sealed class NewFailures {
		@Serializable
		class UserAlreadyExists(val email: String) : NewFailures()
	}

	@Serializable
	class Edit(
		val active: Boolean? = null,
		val administrator: Boolean? = null,
	)

	@Serializable
	sealed class EditFailures {
		@Serializable
		object CannotEditYourself : EditFailures()
	}

	@Serializable
	class SetPassword(
		val oldPassword: String,
		val newPassword: String,
	)

	@Serializable
	class LogIn(
		val email: String,
		val password: String,
	)

	@Serializable
	object LogInFailures

	@Serializable
	object GetFailures

	@Serializable
	object DepartmentListFailures

	@Serializable
	object DepartmentAddFailures

	@Serializable
	object DepartmentRemoveFailures

	@Serializable
	object PasswordResetFailures

	@Serializable
	sealed class PasswordSetFailures {
		@Serializable
		object CanOnlySetYourOwnPassword : PasswordSetFailures()

		@Serializable
		object IncorrectOldPassword : PasswordSetFailures()
	}

	@Serializable
	object TokenVerifyFailures

	@Serializable
	object LogOutFailures
}
