package opensavvy.formulaide.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Input
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.InputDto.Companion.toCore
import opensavvy.formulaide.remote.dto.InputDto.Companion.toDto
import opensavvy.spine.Id

/**
 * DTO for [Field].
 */
@Serializable
sealed class FieldDto {

	abstract val label: String
	abstract val importedFrom: Id?

	/**
	 * DTO for [Field.Label].
	 */
	@Serializable
	@SerialName("LABEL")
	class Label(
		override val label: String,
		override val importedFrom: Id? = null,
	) : FieldDto()

	/**
	 * DTO for [Field.Input].
	 */
	@Serializable
	@SerialName("INPUT")
	class Input(
		override val label: String,
		val input: InputDto,
		override val importedFrom: Id? = null,
	) : FieldDto()

	/**
	 * DTO for [Field.Choice].
	 */
	@Serializable
	@SerialName("CHOICE")
	class Choice(
		override val label: String,
		val options: Map<Int, FieldDto>,
		override val importedFrom: Id? = null,
	) : FieldDto()

	/**
	 * DTO for [Field.Group].
	 */
	@Serializable
	@SerialName("GROUP")
	class Group(
		override val label: String,
		val fields: Map<Int, FieldDto>,
		override val importedFrom: Id? = null,
	) : FieldDto()

	/**
	 * DTO for [Field.Arity].
	 */
	@Serializable
	@SerialName("ARITY")
	class Arity(
		override val label: String,
		val child: FieldDto,
		val min: UInt,
		val max: UInt,
		override val importedFrom: Id? = null,
	) : FieldDto()

	@Serializable
	sealed class CompatibilityFailure {
		@Serializable
		@SerialName("INCOMPATIBLE_FIELD")
		class IncompatibleField(
			val field: String,
			val message: String,
		) : CompatibilityFailure()

		@Serializable
		@SerialName("INCOMPATIBLE_INPUT")
		class IncompatibleInput(
			val field: String,
			val message: String,
		) : CompatibilityFailure()

		@Serializable
		@SerialName("TEMPLATE_NOT_FOUND")
		class TemplateNotFound(
			val field: String,
			val template: Id,
		) : CompatibilityFailure()
	}

	companion object {

		suspend fun FieldDto.toCore(
			decodeTemplate: suspend (Id) -> Template.Version.Ref?,
		): Field = when (this) {
			is Arity -> Field.Arity(
				label,
				child.toCore(decodeTemplate),
				min..max,
				importedFrom?.let { decodeTemplate(it) },
			)

			is Choice -> Field.Choice(
				label,
				options.mapValues { (_, it) -> it.toCore(decodeTemplate) },
				importedFrom?.let { decodeTemplate(it) },
			)

			is Group -> Field.Group(
				label,
				fields.mapValues { (_, it) -> it.toCore(decodeTemplate) },
				importedFrom?.let { decodeTemplate(it) },
			)

			is Input -> Field.Input(
				label,
				input.toCore(),
				importedFrom?.let { decodeTemplate(it) },
			)

			is Label -> Field.Label(
				label,
				importedFrom?.let { decodeTemplate(it) },
			)
		}

		private fun convertTemplateRef(ref: Template.Version.Ref): Id =
			api.templates.id.version.idOf(ref.template.toIdentifier().text, ref.creationDate.toString())

		fun Field.toDto(): FieldDto = when (this) {
			is Field.Arity -> Arity(
				label,
				child.toDto(),
				allowed.first,
				allowed.last,
				importedFrom?.let(::convertTemplateRef),
			)

			is Field.Choice -> Choice(
				label,
				indexedFields.mapValues { (_, it) -> it.toDto() },
				importedFrom?.let(::convertTemplateRef),
			)

			is Field.Group -> Group(
				label,
				indexedFields.mapValues { (_, it) -> it.toDto() },
				importedFrom?.let(::convertTemplateRef),
			)

			is Field.Input -> Input(
				label,
				input.toDto(),
				importedFrom?.let(::convertTemplateRef),
			)

			is Field.Label -> Label(
				label,
				importedFrom?.let(::convertTemplateRef),
			)
		}

		suspend fun CompatibilityFailure.toCore(templates: Template.Service) = when (this) {
			is CompatibilityFailure.IncompatibleField -> Field.Failures.Compatibility.IncompatibleField(
				field = Field.Id.fromString(field),
				message = message,
			)

			is CompatibilityFailure.IncompatibleInput -> Field.Failures.Compatibility.IncompatibleInput(
				field = Field.Id.fromString(field),
				failure = opensavvy.formulaide.core.Input.Failures.Compatibility(message),
			)

			is CompatibilityFailure.TemplateNotFound -> Field.Failures.Compatibility.TemplateNotFound(
				field = Field.Id.fromString(field),
				id = templates.versions.fromIdentifier(api.templates.id.version.identifierOf(template)),
			)
		}

		fun Field.Failures.Compatibility.toDto() = when (this) {
			is Field.Failures.Compatibility.IncompatibleField -> CompatibilityFailure.IncompatibleField(
				field = field.toString(),
				message = message,
			)

			is Field.Failures.Compatibility.IncompatibleInput -> CompatibilityFailure.IncompatibleInput(
				field = field.toString(),
				message = failure.message,
			)

			is Field.Failures.Compatibility.TemplateNotFound -> CompatibilityFailure.TemplateNotFound(
				field = field.toString(),
				template = api.templates.id.version.idOf(id.template.toIdentifier().text, id.creationDate.toString()),
			)
		}
	}
}
