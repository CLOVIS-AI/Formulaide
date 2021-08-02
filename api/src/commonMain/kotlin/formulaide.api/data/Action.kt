package formulaide.api.data

import formulaide.api.fields.FormRoot
import formulaide.api.types.OrderedListElement
import formulaide.api.types.Ref
import formulaide.api.types.Referencable
import formulaide.api.types.ReferenceId
import formulaide.api.users.Service
import kotlinx.serialization.Serializable

/**
 * A step taken to validate a [form submission][FormSubmission].
 * Only employees have access to actions (see [Form.actions]).
 */
@Serializable
data class Action(
	override val id: ReferenceId,
	override val order: Int,
	val reviewer: Ref<Service>,
	val name: String,
	val fields: FormRoot? = null,
) : OrderedListElement, Referencable
