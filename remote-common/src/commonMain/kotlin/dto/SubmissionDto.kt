package opensavvy.formulaide.remote.dto

import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.remote.api
import opensavvy.spine.Id

/**
 * DTO for [Submission].
 */
@Serializable
class SubmissionDto(
	val form: Id,
	val step: Int?,
	val data: Map<String, String>,
)

//region Conversion

suspend fun SubmissionDto.toCore(forms: Form.Service) = Submission(
	form = forms.versions.fromIdentifier(api.forms.id.version.identifierOf(form)),
		formStep = step,
		data = data.mapKeys { (id, _) -> Field.Id.fromString(id) },
	)

fun Submission.toDto() = SubmissionDto(
	form = api.forms.id.version.idOf(form.form.toIdentifier().text, form.creationDate.toString()),
	step = formStep,
	data = data.mapKeys { (id, _) -> id.toString() },
)

//endregion
