package formulaide.core.form

import formulaide.core.Department
import formulaide.core.field.FlatField
import formulaide.core.form.Form.Version
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref

/**
 * A form.
 *
 * End users fill in the [initial fields][Version.fields], after which their submissions are sent to the [review phase][Version.reviewSteps].
 * Reviews are consecutive steps in which employees of a specific [Department] make a decision on the user's submission.
 */
@Serializable
data class Form(
	val id: String,
	val name: String,
	val versions: List<Version>,
	val public: Boolean,
	val open: Boolean,
) {

	@Serializable
	data class Version(
		override val creationDate: Instant,
		override val title: String,
		override val fields: @Contextual FlatField.Container.Ref,
		val reviewSteps: List<ReviewStep>,
	) : AbstractVersion()

	@Serializable
	data class ReviewStep(
		val id: String,
		val order: Int,
		val reviewer: Ref<Department>,
		val title: String,
		val fields: @Contextual FlatField.Container.Ref?,
	)
}
