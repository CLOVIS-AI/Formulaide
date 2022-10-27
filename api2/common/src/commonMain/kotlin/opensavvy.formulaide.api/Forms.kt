package opensavvy.formulaide.api

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import opensavvy.spine.Id
import opensavvy.spine.Parameters

@Serializable
class Form(
	val name: String,
	val public: Boolean,
	val open: Boolean,
	val versions: List<Id>,
) {

	@Serializable
	class Version(
		val creationDate: Instant,
		val title: String,
		val field: Field,
		val steps: List<Step>,
	)

	@Serializable
	class Step(
		val id: Int,
		val department: Id,
		val field: Field?,
	)

	class GetParams : Parameters() {
		var includeClosed by parameter("includeClosed", default = false)
		var includePrivate by parameter("includePrivate", default = false)
	}

	@Serializable
	class New(
		val name: String,
		val public: Boolean,
		val firstVersion: List<Id>,
	)

	@Serializable
	class Edit(
		val name: String?,
		val public: Boolean?,
		val open: Boolean?,
	)
}
