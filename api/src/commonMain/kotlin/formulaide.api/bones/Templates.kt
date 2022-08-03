package formulaide.api.bones

import formulaide.core.form.Template
import kotlinx.serialization.Serializable

@Serializable
class ApiNewTemplate(
	val name: String,
	val firstVersion: Template.Version,
)

@Serializable
class ApiTemplateEdition(
	val name: String? = null,
)
