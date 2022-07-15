package formulaide.core.form

import formulaide.core.field.FlatField
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * A template is a grouping of fields that can be reused from one [Form] to another.
 *
 * Templates can only be used inside other templates.
 */
@Serializable
data class Template(
	val id: String,
	val name: String,
	val versions: List<Version>,
) {

	@Serializable
	data class Version(
		override val creationDate: Instant,
		override val title: String,
		override val fields: @Contextual FlatField.Container.Ref,
	) : AbstractVersion()
}
