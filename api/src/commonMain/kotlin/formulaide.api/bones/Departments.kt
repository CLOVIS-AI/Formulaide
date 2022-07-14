package formulaide.api.bones

import formulaide.api.users.Service
import formulaide.core.Department
import kotlinx.serialization.Serializable

/**
 * API model for [Department].
 */
@Serializable
data class ApiDepartment(
	val id: Int,
	val name: String,
	val open: Boolean,
) {

	companion object {
		fun ApiDepartment.toCore() = Department(
			id.toString(),
			name,
			open,
		)

		fun ApiDepartment.toLegacy() = Service(
			id.toString(),
			name,
			open
		)

		fun Department.toApi() = ApiDepartment(
			id.toInt(),
			name,
			open,
		)
	}
}
