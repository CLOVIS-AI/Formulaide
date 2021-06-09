package formulaide.api.data

import kotlinx.serialization.Serializable

@Serializable
data class FormSubmission(
	val form: FormId,
	val data: Map<String, String?>,
)
