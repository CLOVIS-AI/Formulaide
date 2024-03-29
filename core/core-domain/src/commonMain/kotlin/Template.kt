package opensavvy.formulaide.core

import arrow.core.Nel
import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.Template.Ref
import opensavvy.formulaide.core.Template.Version
import opensavvy.formulaide.core.data.StandardNotFound
import opensavvy.formulaide.core.data.StandardUnauthenticated
import opensavvy.formulaide.core.data.StandardUnauthorized
import opensavvy.formulaide.core.utils.IdentifierParser
import opensavvy.formulaide.core.utils.IdentifierWriter
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

	interface Ref : opensavvy.backbone.Ref<Failures.Get, Template>, IdentifierWriter {

		/**
		 * Edits this template.
		 */
		suspend fun edit(name: String? = null, open: Boolean? = null): Outcome<Failures.Edit, Unit>

		/**
		 * Renames this template.
		 *
		 * @see Template.name
		 */
		suspend fun rename(name: String): Outcome<Failures.Edit, Unit> = edit(name = name)

		/**
		 * Opens this template.
		 *
		 * @see Template.open
		 */
		suspend fun open(): Outcome<Failures.Edit, Unit> = edit(open = true)

		/**
		 * Closes this template.
		 *
		 * @see Template.open
		 */
		suspend fun close(): Outcome<Failures.Edit, Unit> = edit(open = false)

		/**
		 * Creates a new version of this template, named [title] with [field].
		 *
		 * @see Template.versions
		 */
		suspend fun createVersion(title: String, field: Field): Outcome<Failures.CreateVersion, Version.Ref>

		/**
		 * Creates a reference to the version created at [creationDate].
		 *
		 * No verification is done that the version actually exists.
		 */
		fun versionOf(creationDate: Instant): Version.Ref
	}

	data class Version(
		val creationDate: Instant,
		val title: String,
		val field: Field,
	) {

		interface Ref : opensavvy.backbone.Ref<Failures.Get, Version>, IdentifierWriter {
			val creationDate: Instant
			val template: Template.Ref
		}

		interface Service : Backbone<Ref, Failures.Get, Version>, IdentifierParser<Ref>

		sealed interface Failures {
			sealed interface Get : Failures

			data class NotFound(override val id: Ref) : StandardNotFound<Ref>,
				Get

			object Unauthenticated : StandardUnauthenticated,
				Get
		}
	}

	interface Service : Backbone<Ref, Failures.Get, Template>, IdentifierParser<Ref> {

		val versions: Version.Service

		/**
		 * Lists all templates.
		 *
		 * Only employees can access templates (open or closed).
		 */
		suspend fun list(includeClosed: Boolean = false): Outcome<Failures.List, List<Ref>>

		/**
		 * Creates a new template.
		 *
		 * Only administrators can create templates.
		 */
		suspend fun create(
			name: String,
			initialVersionTitle: String,
			field: Field,
		): Outcome<Failures.Create, Ref>
	}

	sealed interface Failures {
		sealed interface Get : Failures
		sealed interface List : Failures
		sealed interface Create : Failures
		sealed interface CreateVersion : Failures, Create
		sealed interface Edit : Failures

		data class NotFound(override val id: Ref) : StandardNotFound<Ref>,
			Get,
			Edit,
			CreateVersion

		object Unauthenticated : StandardUnauthenticated,
			Get,
			List,
			Create,
			CreateVersion,
			Edit

		object Unauthorized : StandardUnauthorized,
			List,
			Create,
			CreateVersion,
			Edit

		data class InvalidImport(
			val failures: Nel<Field.Failures.Compatibility>,
		) : Create,
			CreateVersion
	}
}
