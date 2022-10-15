package formulaide.core

import opensavvy.backbone.Backbone
import opensavvy.state.State

/**
 * A company department, for example "Human resources" or "IT management".
 */
data class Department(
	val id: String,
	val name: String,
	val open: Boolean,
) {

	data class Ref(val id: String, override val backbone: DepartmentBackbone) : opensavvy.backbone.Ref<Department> {
		override fun toString() = "Department $id"
	}
}

interface DepartmentBackbone : Backbone<Department> {

	/**
	 * Lists all departments.
	 *
	 * Setting [includeClosed] to `true` requires administrator authentication.
	 * In that case, closed departments are included in the results.
	 * Otherwise, only open departments are returned.
	 */
	fun all(includeClosed: Boolean = false): State<List<Department.Ref>>

	/**
	 * Creates a new department.
	 *
	 * Requires administrator authentication.
	 */
	fun create(name: String): State<Department.Ref>

	/**
	 * Opens the [department] (sets [Department.open] to `true`).
	 *
	 * Requires administrator authentication.
	 *
	 * If the department is already open, does nothing.
	 */
	fun open(department: Department.Ref): State<Unit>

	/**
	 * Closes the [department] (sets [Department.open] to `false`).
	 *
	 * Requires administrator authentication.
	 *
	 * If the department is already closed, does nothing.
	 */
	fun close(department: Department.Ref): State<Unit>
}
