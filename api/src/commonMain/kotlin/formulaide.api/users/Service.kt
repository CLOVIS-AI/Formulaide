package formulaide.api.users

import formulaide.api.types.Ref
import formulaide.api.types.Referencable
import formulaide.api.types.ReferenceId
import kotlinx.serialization.Serializable

/**
 * Represents a service, for example "Human Resources" or "IT management".
 */
@Serializable
data class Service(
	override val id: ReferenceId,
	val name: String,
	val open: Boolean,
) : Referencable

/**
 * Request to edit a service.
 */
@Serializable
data class ServiceModification(
	val id: Ref<Service>,
	val open: Boolean,
)
