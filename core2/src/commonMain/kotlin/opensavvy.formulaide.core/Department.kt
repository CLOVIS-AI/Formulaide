package opensavvy.formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.state.State

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
		fun open(): State<Unit> = backbone.open(this)

		/** See [AbstractDepartments.close]. */
		fun close(): State<Unit> = backbone.close(this)
	}
}

interface AbstractDepartments : Backbone<Department> {

	/**
	 * Lists all departments.
	 *
	 * If [includeClosed] is `true`, requires administrator authentication.
	 * Otherwise, closed departments are not included in the results.
	 */
	fun list(includeClosed: Boolean = false): State<List<Department.Ref>>

	/**
	 * Creates a new department named [name].
	 *
	 * Requires administrator authentication.
	 */
	fun create(name: String): State<Department.Ref>

	/**
	 * Opens the [department].
	 *
	 * Requires administrator authentication.
	 * If the department is already open, does nothing.
	 */
	fun open(department: Department.Ref): State<Unit>

	/**
	 * Closes the [department].
	 *
	 * Requires administrator authentication.
	 * If the department is already closed, does nothing.
	 */
	fun close(department: Department.Ref): State<Unit>

}
