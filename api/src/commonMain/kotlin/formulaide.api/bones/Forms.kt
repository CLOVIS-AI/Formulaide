package formulaide.api.bones

import formulaide.core.form.Form
import kotlinx.serialization.Serializable

@Serializable
class ApiNewForm(
	val name: String,
	val firstVersion: Form.Version,
	val public: Boolean,
)

@Serializable
class ApiFormEdition(
	val name: String? = null,
	val public: Boolean? = null,
	val open: Boolean? = null,
)
