package opensavvy.formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.state.slice.Slice

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

	data class Ref(
		val id: String,
		override val backbone: Service,
	) : opensavvy.backbone.Ref<Department> {

		/**
		 * Makes this department visible.
		 *
		 * @see Department.open
		 * @see Service.edit
		 */
		suspend fun open() = backbone.edit(this, open = true)

		/**
		 * Makes this department invisible.
		 *
		 * @see Department.open
		 * @see Service.edit
		 */
		suspend fun close() = backbone.edit(this, open = false)

		override fun toString() = "Département $id"
	}

	interface Service : Backbone<Department> {

		/**
		 * Lists all departments.
		 *
		 * If [includeClosed] is `true`, requires administrator authentication.
		 * Otherwise, closed departments are not returned.
		 */
		suspend fun list(includeClosed: Boolean = false): Slice<List<Ref>>

		/**
		 * Creates a new department named [name].
		 *
		 * Requires administrator authentication.
		 */
		suspend fun create(name: String): Slice<Ref>

		/**
		 * Edits the [department].
		 *
		 * Requires administrator authentication.
		 *
		 * @param open Allows the edit the visibility of the department (see [Department.open]).
		 */
		suspend fun edit(
			department: Ref,
			open: Boolean? = null,
		): Slice<Unit>
	}
}
