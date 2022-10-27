package opensavvy.formulaide.core

import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.state.State

class Template(
	val name: String,
	val versions: List<Version.Ref>,
	val open: Boolean,
) {

	data class Ref(
		val id: String,
		override val backbone: AbstractTemplates,
	) : opensavvy.backbone.Ref<Template> {

		override fun toString() = "Modèle $id"
	}

	data class Version(
		val creationDate: Instant,
		val title: String,
		val field: Field,
	) {

		data class Ref(
			val template: Template.Ref,
			val version: Instant,
			override val backbone: AbstractTemplateVersions,
		) : opensavvy.backbone.Ref<Version> {

			override fun toString() = "$template $version"
		}
	}
}

interface AbstractTemplates : Backbone<Template> {

	/**
	 * Lists all templates.
	 */
	fun list(includeClosed: Boolean = false): State<List<Template.Ref>>

	/**
	 * Creates a new template.
	 *
	 * Only administrators can create templates.
	 */
	fun create(name: String, firstVersion: Template.Version): State<Template.Ref>

	/**
	 * Creates a new version of a given template.
	 *
	 * Only administrators can create new versions.
	 */
	fun createVersion(template: Template.Ref, version: Template.Version): State<Template.Version.Ref>

	/**
	 * Edits a form.
	 *
	 * Only administrators can edit forms.
	 */
	fun edit(template: Template.Ref, name: String? = null, open: Boolean? = null): State<Unit>

}

interface AbstractTemplateVersions : Backbone<Template.Version>
