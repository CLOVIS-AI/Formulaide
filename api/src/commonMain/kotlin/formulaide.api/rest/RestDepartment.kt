package formulaide.api.rest

import formulaide.core.Department
import kotlinx.serialization.Serializable
import opensavvy.spine.Parameters

@Serializable
class RestDepartment(
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

	fun toCore(id: String) = Department(
		id,
		name,
		open,
	)
}

fun Department.toRest() = RestDepartment(
	name,
	open,
)
