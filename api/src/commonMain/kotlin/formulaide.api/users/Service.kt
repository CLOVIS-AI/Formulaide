package formulaide.api.users

import kotlinx.serialization.Serializable

/**
 * The ID of a [Service].
 */
typealias ServiceId = Int

/**
 * Represents a service, for example "Human Resources" or "IT management".
 */
@Serializable
data class Service(
	val id: ServiceId,
	val name: String,
	val open: Boolean,
)

/**
 * Request to edit a service.
 */
@Serializable
data class ServiceModification(
	val id: ServiceId,
	val open: Boolean,
)
