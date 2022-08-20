package formulaide.core.form

import formulaide.core.Department
import formulaide.core.field.FlatField
import formulaide.core.form.Form.Version
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Backbone

/**
 * A form.
 *
 * End users fill in the [initial fields][Version.fields], after which their submissions are sent to the [review phase][Version.reviewSteps].
 * Reviews are consecutive steps in which employees of a specific [Department] make a decision on the user's submission.
 */
@Serializable
data class Form(
	@SerialName("_id") val id: String,
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
		val id: Int,
		val reviewer: @Contextual Department.Ref,
		val title: String,
		val fields: @Contextual FlatField.Container.Ref?,
	)

	data class Ref(val id: String, override val backbone: FormBackbone) : opensavvy.backbone.Ref<Form> {
		override fun toString() = "Form $id"
	}
}

interface FormBackbone : Backbone<Form> {
	/**
	 * Lists all forms.
	 *
	 * - [includeClosed]: requires administrator authentication
	 */
	suspend fun all(includeClosed: Boolean = false): List<Form.Ref>

	/**
	 * Creates a form.
	 *
	 * Only administrators can create forms.
	 */
	suspend fun create(name: String, firstVersion: Version, public: Boolean): Form.Ref

	/**
	 * Adds a new version to a [form].
	 *
	 * Only administrators can edit forms.
	 */
	suspend fun createVersion(form: Form.Ref, new: Version)

	/**
	 * Edits a [form].
	 *
	 * Only administrators can edit forms.
	 */
	suspend fun edit(form: Form.Ref, name: String? = null, public: Boolean? = null, open: Boolean? = null)
}
