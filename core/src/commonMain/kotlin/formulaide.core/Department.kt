package formulaide.core

import kotlinx.serialization.Serializable
import opensavvy.backbone.Backbone

/**
 * A company department, for example "Human resources" or "IT management".
 */
@Serializable
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
	suspend fun all(includeClosed: Boolean = false): List<Department.Ref>

	/**
	 * Creates a new department.
	 *
	 * Requires administrator authentication.
	 */
	suspend fun create(name: String): Department.Ref

	/**
	 * Opens the [department] (sets [Department.open] to `true`).
	 *
	 * Requires administrator authentication.
	 *
	 * If the department is already open, does nothing.
	 */
	suspend fun open(department: Department.Ref)

	/**
	 * Closes the [department] (sets [Department.open] to `false`).
	 *
	 * Requires administrator authentication.
	 *
	 * If the department is already closed, does nothing.
	 */
	suspend fun close(department: Department.Ref)
}
