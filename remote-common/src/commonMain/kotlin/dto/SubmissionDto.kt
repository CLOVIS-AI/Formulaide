package opensavvy.formulaide.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Input
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
) {

	@Serializable
	object GetFailures

	@Serializable
	sealed class ParsingFailures {
		@Serializable
		@SerialName("FORM_VERSION_NOT_FOUND")
		object FormVersionNotFound : ParsingFailures()

		@Serializable
		@SerialName("MISSING_FIELD")
		class Mandatory(
			val field: String,
		) : ParsingFailures()

		@Serializable
		@SerialName("INVALID_INPUT")
		class InvalidInput(
			val field: String,
			val failure: String,
		) : ParsingFailures()

		@Serializable
		@SerialName("SELECTED_CHOICE_MATCHES_NO_OPTION")
		class SelectedChoiceMatchesNoOptions(
			val field: String,
			val selected: String,
			val valid: List<Int>,
		) : ParsingFailures()

		@Serializable
		@SerialName("MISSING_GROUP_MARKER")
		class MissingGroupMarker(
			val field: String,
		) : ParsingFailures()
	}
}

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

fun SubmissionDto.ParsingFailures.toCore() = when (this) {
	is SubmissionDto.ParsingFailures.FormVersionNotFound -> Submission.ParsingFailure.UnavailableForm
	is SubmissionDto.ParsingFailures.InvalidInput -> Submission.ParsingFailure.InvalidValue.InputParsingFailure(Field.Id.fromString(field), Input.Failures.Parsing(failure))
	is SubmissionDto.ParsingFailures.Mandatory -> Submission.ParsingFailure.InvalidValue.Mandatory(Field.Id.fromString(field))
	is SubmissionDto.ParsingFailures.MissingGroupMarker -> Submission.ParsingFailure.InvalidValue.MissingGroupMarker(Field.Id.fromString(field))
	is SubmissionDto.ParsingFailures.SelectedChoiceMatchesNoOptions -> Submission.ParsingFailure.InvalidValue.SelectedChoiceMatchesNoOptions(Field.Id.fromString(field), selected, valid)
}

fun Submission.ParsingFailure.toDto() = when (this) {
	is Submission.ParsingFailure.InvalidValue.InputParsingFailure -> SubmissionDto.ParsingFailures.InvalidInput(field.toString(), failure.message)
	is Submission.ParsingFailure.InvalidValue.Mandatory -> SubmissionDto.ParsingFailures.Mandatory(field.toString())
	is Submission.ParsingFailure.InvalidValue.MissingGroupMarker -> SubmissionDto.ParsingFailures.MissingGroupMarker(field.toString())
	is Submission.ParsingFailure.InvalidValue.SelectedChoiceMatchesNoOptions -> SubmissionDto.ParsingFailures.SelectedChoiceMatchesNoOptions(field.toString(), selected, valid.toList())
	is Submission.ParsingFailure.UnavailableForm -> SubmissionDto.ParsingFailures.FormVersionNotFound
}

//endregion
