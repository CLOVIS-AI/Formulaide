package opensavvy.formulaide.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toCore
import opensavvy.formulaide.remote.dto.FieldDto.Companion.toDto
import opensavvy.spine.Id
import opensavvy.spine.Parameters

/**
 * DTO for [Template] and [Form].
 */
@Serializable
class SchemaDto(
	val name: String,
	val open: Boolean,
	val public: Boolean,
	val versions: List<Id>,
) {

	/**
	 * DTO for [Template.Version] and [Form.Version].
	 */
	@Serializable
	class Version(
		val creationDate: Instant,
		val title: String,
		val field: FieldDto,
		val steps: List<Step>? = null,
	)

	/**
	 * DTO for [Form.Step].
	 */
	@Serializable
	class Step(
		val id: Int,
		val name: String,
		val reviewer: Id,
		val field: FieldDto?,
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
		val name: String? = null,
		val open: Boolean? = null,
		val public: Boolean? = null,
	)

	companion object {

		//region Template conversion

		suspend fun Version.toTemplateVersion(
			decodeTemplate: suspend (Id) -> Template.Version.Ref?,
		) = Template.Version(
			creationDate = creationDate,
			title = title,
			field = field.toCore(decodeTemplate),
		)

		fun Template.Version.toDto() = Version(
			creationDate = creationDate,
			title = title,
			field = field.toDto(),
		)

		private fun convertTemplateRef(ref: Template.Version.Ref): Id =
			api.templates.id.version.idOf(ref.template.id, ref.version.toString())

		suspend fun SchemaDto.toTemplate(
			decodeTemplate: suspend (Id) -> Template.Version.Ref?,
		) = Template(
			name = name,
			versions = versions.map { decodeTemplate(it) ?: error("Couldn't decode the template version ID $it") },
			open = open,
		)

		fun Template.toDto() = SchemaDto(
			name = name,
			open = open,
			public = false,
			versions = versions.map { convertTemplateRef(it) },
		)

		//endregion
		//region Form conversion

		suspend fun Step.toCore(
			decodeDepartment: suspend (Id) -> Department.Ref,
			decodeTemplate: suspend (Id) -> Template.Version.Ref?,
		) = Form.Step(
			id = id,
			name = name,
			reviewer = decodeDepartment(reviewer),
			field = field?.toCore(decodeTemplate),
		)

		suspend fun Version.toForm(
			decodeDepartment: suspend (Id) -> Department.Ref,
			decodeTemplate: suspend (Id) -> Template.Version.Ref?,
		) = Form.Version(
			creationDate,
			title,
			field.toCore(decodeTemplate),
			(steps ?: emptyList()).map { it.toCore(decodeDepartment, decodeTemplate) },
		)

		suspend fun SchemaDto.toForm(
			decodeForm: suspend (Id) -> Form.Version.Ref,
		) = Form(
			name = name,
			versions = versions.map { decodeForm(it) },
			open = open,
			public = public,
		)

		private fun convertFormRef(ref: Form.Version.Ref): Id =
			api.templates.id.version.idOf(ref.form.id, ref.version.toString())

		fun Form.Step.toDto() = Step(
			id,
			name,
			api.departments.id.idOf(reviewer.id),
			field?.toDto(),
		)

		fun Form.Version.toDto() = Version(
			creationDate,
			title,
			field.toDto(),
			steps.map { it.toDto() },
		)

		fun Form.toDto() = SchemaDto(
			name,
			open,
			public,
			versions.map { convertFormRef(it) },
		)

		//endregion

	}
}
