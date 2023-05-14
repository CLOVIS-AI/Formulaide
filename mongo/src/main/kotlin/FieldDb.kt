package opensavvy.formulaide.mongo

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.mongo.InputDbDto.Companion.toCore
import opensavvy.formulaide.mongo.InputDbDto.Companion.toDto

@Serializable
sealed class FieldDbDto {

    abstract val label: String
    abstract val importedFromTemplate: String?
    abstract val importedFromTemplateVersion: Instant?

    @Serializable
    class Label(
        override val label: String,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : FieldDbDto()

    @Serializable
    class Input(
        override val label: String,
        val input: InputDbDto,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : FieldDbDto()

    @Serializable
    class Choice(
        override val label: String,
        val options: Map<Int, FieldDbDto>,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : FieldDbDto()

    @Serializable
    class Group(
        override val label: String,
        val fields: Map<Int, FieldDbDto>,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : FieldDbDto()

    @Serializable
    class Arity(
        override val label: String,
        val child: FieldDbDto,
        val min: UInt,
        val max: UInt,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : FieldDbDto()

    companion object {

        fun FieldDbDto.toCore(
            templates: Template.Service,
        ): Field {
            val importedFrom = if (importedFromTemplate != null && importedFromTemplateVersion != null)
                templates.fromIdentifier(importedFromTemplate!!).versionOf(importedFromTemplateVersion!!)
            else
                null

            return when (this) {
                is Arity -> Field.Arity(
                    label,
                    child.toCore(templates),
                    min..max,
                    importedFrom,
                )

                is Choice -> Field.Choice(
                    label,
                    options.mapValues { (_, it) -> it.toCore(templates) },
                    importedFrom,
                )

                is Group -> Field.Group(
                    label,
                    fields.mapValues { (_, it) -> it.toCore(templates) },
                    importedFrom,
                )

                is Input -> Field.Input(
                    label,
                    input.toCore(),
                    importedFrom,
                )

                is Label -> Field.Label(
                    label,
                    importedFrom,
                )
            }
        }

        fun Field.toDto(): FieldDbDto = when (this) {
            is Field.Arity -> Arity(
                label,
                child.toDto(),
                allowed.first,
                allowed.last,
                importedFrom?.template?.toIdentifier()?.text,
                importedFrom?.creationDate,
            )

            is Field.Choice -> Choice(
                label,
                indexedFields.mapValues { (_, it) -> it.toDto() },
                importedFrom?.template?.toIdentifier()?.text,
                importedFrom?.creationDate,
            )

            is Field.Group -> Group(
                label,
                indexedFields.mapValues { (_, it) -> it.toDto() },
                importedFrom?.template?.toIdentifier()?.text,
                importedFrom?.creationDate,
            )

            is Field.Input -> Input(
                label,
                input.toDto(),
                importedFrom?.template?.toIdentifier()?.text,
                importedFrom?.creationDate,
            )

            is Field.Label -> Label(
                label,
                importedFrom?.template?.toIdentifier()?.text,
                importedFrom?.creationDate,
            )
        }

    }

}
