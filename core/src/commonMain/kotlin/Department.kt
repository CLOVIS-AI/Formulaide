package opensavvy.formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.formulaide.core.data.StandardNotFound
import opensavvy.formulaide.core.data.StandardUnauthenticated
import opensavvy.formulaide.core.data.StandardUnauthorized
import opensavvy.formulaide.core.utils.IdentifierParser
import opensavvy.formulaide.core.utils.IdentifierWriter
import opensavvy.state.outcome.Outcome

/**
 * A department of the organisation, for example "Human resources" or "IT support".
 */
data class Department(
	/**
	 * The display name of this department.
	 */
	val name: String,

	/**
	 * Whether this department is currently open.
	 *
	 * Closed departments are hidden by default in the UI.
	 * Only administrators can see closed departments.
	 */
	val open: Boolean,
) {

	interface Ref : opensavvy.backbone.Ref<Failures.Get, Department>, IdentifierWriter {

		/**
		 * Makes this department visible.
		 *
		 * Requires administrator authentication.
		 *
		 * @see Department.open
		 */
		suspend fun open(): Outcome<Failures.Edit, Unit>

		/**
		 * Makes this department invisible.
		 *
		 * Requires administrator authentication.
		 *
		 * @see Department.open
		 */
		suspend fun close(): Outcome<Failures.Edit, Unit>
	}

	interface Service<R : Ref> : Backbone<R, Failures.Get, Department>, IdentifierParser<R> {

		/**
		 * Lists all departments.
		 *
		 * If [includeClosed] is `true`, requires administrator authentication.
		 * Otherwise, closed departments are not returned.
		 */
		suspend fun list(includeClosed: Boolean = false): Outcome<Failures.List, List<R>>

		/**
		 * Creates a new department named [name].
		 *
		 * Requires administrator authentication.
		 */
		suspend fun create(name: String): Outcome<Failures.Create, R>
	}

	sealed interface Failures {
		sealed interface Get : Failures
		sealed interface List : Failures
		sealed interface Create : Failures
		sealed interface Edit : Failures

		data class NotFound(override val id: Ref) : StandardNotFound<Ref>,
			Get,
			Edit

		object Unauthenticated : StandardUnauthenticated,
			Get,
			Create,
			Edit,
			List

		object Unauthorized : StandardUnauthorized,
			Create,
			Edit,
			List
	}
}
