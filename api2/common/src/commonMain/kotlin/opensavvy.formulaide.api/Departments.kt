package opensavvy.formulaide.api

import kotlinx.serialization.Serializable
import opensavvy.spine.Parameters

@Serializable
class Department(
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
	class EditVisibility(
		val open: Boolean,
	)

}
