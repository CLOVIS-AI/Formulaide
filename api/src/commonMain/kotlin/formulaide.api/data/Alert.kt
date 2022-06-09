package formulaide.api.data

import formulaide.api.types.Ref
import formulaide.api.users.User
import kotlinx.serialization.Serializable

/**
 * Security events that the administrators should look into.
 */
@Serializable
data class Alert(
	val level: Level,
	val timestamp: Long,
	val message: String,
	val user: Ref<User>?,
) {
	@Serializable
	enum class Level {
		Low,
		Medium,
		High
	}
}
