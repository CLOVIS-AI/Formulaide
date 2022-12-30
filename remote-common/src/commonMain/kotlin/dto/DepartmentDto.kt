package opensavvy.formulaide.remote.dto

import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Department
import opensavvy.spine.Parameters

@Serializable
data class DepartmentDto(
	val name: String,
	val open: Boolean,
) {

	class GetParams : Parameters() {
		var includeClosed by parameter("includeClosed", default = false)
	}

	@Serializable
	class New(
		val name: String,
	)

	@Serializable
	class Edit(
		val open: Boolean? = null,
	)

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
