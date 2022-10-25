package opensavvy.formulaide.api

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import opensavvy.spine.Id
import opensavvy.spine.Parameters

@Serializable
class Template(
	val name: String,
	val open: Boolean,
	val versions: List<Id>,
) {

	@Serializable
	class Version(
		val creationDate: Instant,
		val title: String,
		val field: Field,
	)

	class GetParams : Parameters() {
		var includeClosed by parameter("includeClosed", default = false)
	}

	@Serializable
	class New(
		val name: String,
		val firstVersion: Version,
	)

	@Serializable
	class Edit(
		val name: String?,
		val open: Boolean?,
	)

}
