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

	class GetParams : Parameters() {
		var includeClosed by parameter("includeClosed", default = false)
	}

	@Serializable
	class New(
		val email: String,
		val name: String,
		val administrator: Boolean = false,
	)

	@Serializable
	class Edit(
		val active: Boolean? = null,
		val administrator: Boolean? = null,
	)

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

}
