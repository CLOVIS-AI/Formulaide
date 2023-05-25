package opensavvy.formulaide.remote.dto

import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Department
import opensavvy.spine.Parameters

/**
 * DTO for [Department].
 */
@Serializable
data class DepartmentDto(
	val name: String,
	val open: Boolean,
) {

	class ListParams : Parameters() {
		var includeClosed by parameter("includeClosed", default = false)
	}

	@Serializable
	object ListFailures

	@Serializable
	class New(
		val name: String,
	)

	@Serializable
	object NewFailures

	@Serializable
	class Edit(
		val open: Boolean? = null,
	)

	@Serializable
	object EditFailures

	@Serializable
	object GetFailures

	companion object {

		fun DepartmentDto.toCore() = Department(
			name = name,
			open = open,
		)

		fun Department.toDto() = DepartmentDto(
			name = name,
			open = open,
		)

	}
}
