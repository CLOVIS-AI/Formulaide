package opensavvy.formulaide.core

import arrow.core.Nel
import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.Template.Ref
import opensavvy.formulaide.core.Template.Version
import opensavvy.state.failure.CustomFailure
import opensavvy.state.failure.Failure
import opensavvy.state.outcome.Outcome
import opensavvy.state.failure.NotFound as StandardNotFound
import opensavvy.state.failure.Unauthenticated as StandardUnauthenticated
import opensavvy.state.failure.Unauthorized as StandardUnauthorized

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

	interface Ref : opensavvy.backbone.Ref<Failures.Get, Template> {

		/**
		 * Renames this template.
		 *
		 * @see Template.name
		 */
		suspend fun rename(name: String): Outcome<Failures.Edit, Unit>

		/**
		 * Opens this template.
		 *
		 * @see Template.open
		 */
		suspend fun open(): Outcome<Failures.Edit, Unit>

		/**
		 * Closes this template.
		 *
		 * @see Template.open
		 */
		suspend fun close(): Outcome<Failures.Edit, Unit>

		/**
		 * Creates a new version of this template, named [title] with [field].
		 *
		 * @see Template.versions
		 */
		suspend fun createVersion(title: String, field: Field): Outcome<Failures.CreateVersion, Version.Ref>
	}

	data class Version(
		val creationDate: Instant,
		val title: String,
		val field: Field,
	) {

		interface Ref : opensavvy.backbone.Ref<Failures.Get, Version> {

			val template: Template.Ref
		}

		interface Service : Backbone<Ref, Failures.Get, Version>

		sealed interface Failures : Failure {
			sealed interface Get : Failures

			class NotFound(val ref: Ref) : CustomFailure(StandardNotFound(ref)),
				Get

			object Unauthenticated : CustomFailure(StandardUnauthenticated()),
				Get
		}
	}

	interface Service : Backbone<Ref, Failures.Get, Template> {

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

	sealed interface Failures : Failure {
		sealed interface Get : Failures
		sealed interface List : Failures
		sealed interface Create : Failures
		sealed interface CreateVersion : Failures
		sealed interface Edit : Failures

		class NotFound(val ref: Ref) : CustomFailure(StandardNotFound(ref)),
			Get,
			Edit,
			CreateVersion

		object Unauthenticated : CustomFailure(StandardUnauthenticated()),
			Get,
			List,
			Create,
			CreateVersion,
			Edit

		object Unauthorized : CustomFailure(StandardUnauthorized()),
			List,
			Create,
			CreateVersion,
			Edit

		class InvalidImport(
			val failures: Nel<Field.Failures.Compatibility>,
		) : CustomFailure(Companion, "Champ incompatible : $failures"),
			Create,
			CreateVersion {
			companion object : Failure.Key
		}
	}
}
