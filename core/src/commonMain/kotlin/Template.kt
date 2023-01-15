package opensavvy.formulaide.core

import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.Template.Ref
import opensavvy.formulaide.core.Template.Version
import opensavvy.state.outcome.Outcome

/**
 * A field container that is reused between [forms][Form].
 *
 * Like forms, templates are versioned, and can be open or closed.
 * Unlike forms, they cannot have review steps.
 *
 * Templates are visible only by authenticated users.
 */
data class Template(
	/**
	 * The human-readable name of this template.
	 *
	 * @see Ref.rename
	 */
	val name: String,

	/**
	 * The various versions of this template.
	 *
	 * @see Version
	 * @see Ref.createVersion
	 */
	val versions: List<Version.Ref>,

	/**
	 * `true` if this template is open.
	 *
	 * A closed template doesn't appear in import suggestions when editing a form.
	 * Authenticated users can access all templates, even if they are closed.
	 */
	val open: Boolean,
) {

	data class Ref(
		val id: String,
		override val backbone: Service,
	) : opensavvy.backbone.Ref<Template> {

		/**
		 * Renames this template.
		 *
		 * @see Template.name
		 * @see Service.edit
		 */
		suspend fun rename(name: String) = backbone.edit(this, name = name)

		/**
		 * Opens this template.
		 *
		 * @see Template.open
		 * @see Service.edit
		 */
		suspend fun open() = backbone.edit(this, open = true)

		/**
		 * Closes this template.
		 *
		 * @see Template.open
		 * @see Service.edit
		 */
		suspend fun close() = backbone.edit(this, open = false)

		/**
		 * Creates a new [version] of this template.
		 *
		 * @see Template.versions
		 * @see Service.createVersion
		 */
		suspend fun createVersion(version: Version) = backbone.createVersion(this, version)

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
			override val backbone: Service,
		) : opensavvy.backbone.Ref<Version> {

			override fun toString() = "$template $version"
		}

		interface Service : Backbone<Version>
	}

	interface Service : Backbone<Template> {

		val versions: Version.Service

		/**
		 * Lists all templates.
		 *
		 * Only employees can access templates (open or closed).
		 */
		suspend fun list(includeClosed: Boolean = false): Outcome<List<Ref>>

		/**
		 * Creates a new template.
		 *
		 * Only administrators can create templates.
		 */
		suspend fun create(name: String, firstVersion: Version): Outcome<Ref>

		/**
		 * Creates a new template.
		 *
		 * Only administrators can create templates.
		 */
		suspend fun create(
			name: String,
			initialVersionTitle: String,
			field: Field,
		) = create(name, Version(Instant.DISTANT_PAST, initialVersionTitle, field))

		/**
		 * Creates a new version of a given template.
		 *
		 * Only administrators can create new versions.
		 */
		suspend fun createVersion(template: Ref, version: Version): Outcome<Version.Ref>

		/**
		 * Edits a template.
		 *
		 * Only administrators can edit template.
		 */
		suspend fun edit(template: Ref, name: String? = null, open: Boolean? = null): Outcome<Unit>
	}
}
