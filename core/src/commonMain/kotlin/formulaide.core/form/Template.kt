package formulaide.core.form

import formulaide.core.field.FlatField
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Backbone

/**
 * A template is a grouping of fields that can be reused from one [Form] to another.
 *
 * Templates can only be used inside other templates.
 */
@Serializable
data class Template(
	@SerialName("_id") val id: String,
	val name: String,
	val versions: List<Version>,
) {

	@Serializable
	data class Version(
		override val creationDate: Instant,
		override val title: String,
		override val fields: @Contextual FlatField.Container.Ref,
	) : AbstractVersion()

	data class Ref(val id: String, override val backbone: TemplateBackbone) : opensavvy.backbone.Ref<Template> {
		override fun toString() = "Form $id"
	}
}

interface TemplateBackbone : Backbone<Template> {
	/**
	 * Lists all the available templates.
	 */
	suspend fun all(): List<Template.Ref>

	/**
	 * Creates a new template.
	 *
	 * Only administrators can create templates.
	 */
	suspend fun create(name: String, firstVersion: Template.Version): Template.Ref

	/**
	 * Creates a new version.
	 *
	 * Only administrators can edit templates.
	 */
	suspend fun createVersion(template: Template.Ref, version: Template.Version)

	/**
	 * Edits a template.
	 *
	 * Only administrators can edit templates.
	 */
	suspend fun edit(template: Template.Ref, name: String? = null)
}
