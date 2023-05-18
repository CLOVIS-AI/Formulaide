package opensavvy.formulaide.mongo

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.mongo.MongoInputDto.Companion.toCore
import opensavvy.formulaide.mongo.MongoInputDto.Companion.toDto

@Serializable
sealed class MongoFieldDto {

    abstract val label: String
    abstract val importedFromTemplate: String?
    abstract val importedFromTemplateVersion: Instant?

    @Serializable
    class Label(
        override val label: String,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : MongoFieldDto()

    @Serializable
    class Input(
        override val label: String,
        val input: MongoInputDto,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : MongoFieldDto()

    @Serializable
    class Choice(
        override val label: String,
        val options: Map<Int, MongoFieldDto>,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : MongoFieldDto()

    @Serializable
    class Group(
        override val label: String,
        val fields: Map<Int, MongoFieldDto>,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : MongoFieldDto()

    @Serializable
    class Arity(
        override val label: String,
        val child: MongoFieldDto,
        val min: UInt,
        val max: UInt,
        override val importedFromTemplate: String? = null,
        override val importedFromTemplateVersion: Instant?,
    ) : MongoFieldDto()

    companion object {

        fun MongoFieldDto.toCore(
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

        fun Field.toDto(): MongoFieldDto = when (this) {
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
