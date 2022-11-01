package opensavvy.formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.state.slice.Slice

/**
 * A company department, for example "Human resources" or "IT management".
 */
data class Department(
	val name: String,
	val open: Boolean,
) {
	data class Ref(val id: String, override val backbone: AbstractDepartments) : opensavvy.backbone.Ref<Department> {
		override fun toString() = "Département $id"

		/** See [AbstractDepartments.open]. */
		suspend fun open() = backbone.open(this)

		/** See [AbstractDepartments.close]. */
		suspend fun close() = backbone.close(this)
	}
}

interface AbstractDepartments : Backbone<Department> {

	/**
	 * Lists all departments.
	 *
	 * If [includeClosed] is `true`, requires administrator authentication.
	 * Otherwise, closed departments are not included in the results.
	 */
	suspend fun list(includeClosed: Boolean = false): Slice<List<Department.Ref>>

	/**
	 * Creates a new department named [name].
	 *
	 * Requires administrator authentication.
	 */
	suspend fun create(name: String): Slice<Department.Ref>

	/**
	 * Opens the [department].
	 *
	 * Requires administrator authentication.
	 * If the department is already open, does nothing.
	 */
	suspend fun open(department: Department.Ref): Slice<Unit>

	/**
	 * Closes the [department].
	 *
	 * Requires administrator authentication.
	 * If the department is already closed, does nothing.
	 */
	suspend fun close(department: Department.Ref): Slice<Unit>

}
