package formulaide.api.types

import kotlinx.serialization.Serializable

/**
 * A properly formatted email.
 */
@Serializable
data class Email(
	val email: String,
) {

	init {
		require(email.isNotBlank()) { "L'email '$email' est vide" }
		require('@' in email) { "L'email '$email' ne contient pas d'arobase (@)" }
	}

}
