package opensavvy.formulaide.api

import kotlinx.serialization.Serializable
import opensavvy.spine.Id
import opensavvy.spine.Parameters

@Serializable
class User(
	val email: String,
	val name: String,
	val open: Boolean,
	val departments: Set<Id>,
	val administrator: Boolean,
	val singleUsePassword: Boolean,
) {

	class GetParams : Parameters() {
		var includeClosed by parameter("includeClosed", default = false)
	}

	@Serializable
	class New(
		val email: String,
		val name: String,
		val departments: Set<Id>,
		val administrator: Boolean,
	)

	@Serializable
	class TemporaryPassword(
		val singleUsePassword: String,
	)

	@Serializable
	class LogInForm(
		val email: String,
		val password: String,
	)

	@Serializable
	class PasswordModification(
		val oldPassword: String,
		val newPassword: String,
	)

	@Serializable
	class Edit(
		val open: Boolean?,
		val administrator: Boolean?,
		val departments: Set<Id>?,
	)
}
