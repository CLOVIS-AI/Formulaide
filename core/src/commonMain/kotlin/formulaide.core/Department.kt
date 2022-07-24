package formulaide.core

import opensavvy.backbone.Backbone

/**
 * A company department, for example "Human resources" or "IT management".
 */
data class Department(
	val id: String,
	val name: String,
	val open: Boolean,
)

interface DepartmentBackbone : Backbone<Department> {

	/**
	 * Lists all departments.
	 *
	 * Setting [includeClosed] to `true` requires administrator authentication.
	 * In that case, closed departments are included in the results.
	 * Otherwise, only open departments are returned.
	 */
	suspend fun all(includeClosed: Boolean = false): List<Ref<Department>>

	/**
	 * Creates a new department.
	 *
	 * Requires administrator authentication.
	 */
	suspend fun create(name: String): Ref<Department>

	/**
	 * Opens the [department] (sets [Department.open] to `true`).
	 *
	 * Requires administrator authentication.
	 *
	 * If the department is already open, does nothing.
	 */
	suspend fun open(department: Ref<Department>)

	/**
	 * Closes the [department] (sets [Department.open] to `false`).
	 *
	 * Requires administrator authentication.
	 *
	 * If the department is already closed, does nothing.
	 */
	suspend fun close(department: Ref<Department>)
}
