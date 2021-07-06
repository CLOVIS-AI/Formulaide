package formulaide.api.data

import formulaide.api.types.OrderedListElement
import formulaide.api.types.Ref
import formulaide.api.types.ReferenceId
import formulaide.api.users.Service
import kotlinx.serialization.Serializable

/**
 * A step taken to validate a [form submission][FormSubmission].
 * Only employees have access to actions (see [Form.actions]).
 */
@Serializable
data class Action(
	val id: ReferenceId,
	override val order: Int,
	val reviewer: Ref<Service>,
) : OrderedListElement
