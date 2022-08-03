package formulaide.api.bones

import formulaide.core.field.FlatField
import kotlinx.serialization.Serializable

@Serializable
class ApiNewFields(
	val name: String,
	val root: FlatField,
)
