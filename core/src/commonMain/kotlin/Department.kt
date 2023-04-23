package opensavvy.formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.state.failure.CustomFailure
import opensavvy.state.failure.Failure
import opensavvy.state.outcome.Outcome
import opensavvy.state.failure.NotFound as StandardNotFound
import opensavvy.state.failure.Unauthenticated as StandardUnauthenticated
import opensavvy.state.failure.Unauthorized as StandardUnauthorized

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

	interface Ref : opensavvy.backbone.Ref<Failures.Get, Department> {

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

	interface Service<R : Ref> : Backbone<R, Failures.Get, Department> {

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

	sealed interface Failures : Failure {
		sealed interface Get : Failures
		sealed interface List : Failures
		sealed interface Create : Failures
		sealed interface Edit : Failures

		class NotFound(val ref: Ref) : CustomFailure(StandardNotFound(ref)),
			Get,
			Edit

		object Unauthenticated : CustomFailure(StandardUnauthenticated()),
			Get,
			Create,
			Edit,
			List

		object Unauthorized : CustomFailure(StandardUnauthorized()),
			Create,
			Edit,
			List
	}
}
