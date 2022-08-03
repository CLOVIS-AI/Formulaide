package formulaide.api.users

import formulaide.api.types.Referencable
import formulaide.api.types.ReferenceId
import formulaide.core.Department
import kotlinx.serialization.Serializable

/**
 * Represents a service, for example "Human Resources" or "IT management".
 *
 * > This object is part of the legacy API.
 * > See [Department].
 */
@Serializable
data class Service(
	override val id: ReferenceId,
	val name: String,
	val open: Boolean,
) : Referencable
