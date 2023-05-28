package opensavvy.formulaide.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.core.User
import opensavvy.formulaide.remote.api
import opensavvy.spine.Id

/**
 * DTO for [Record].
 */
@Serializable
class RecordDto(
	val form: Id,
	val createdAt: Instant,
	val modifiedAt: Instant,
	val status: Status,
	val history: List<Diff>,
) {

	/**
	 * DTO for [Record.QueryCriterion].
	 */
	@Serializable
	sealed class Criterion

	/**
	 * DTO for [Record.Diff].
	 */
	@Serializable
	class Diff(
		val type: Type,
		val author: Id?,
		val source: Status,
		val target: Status,
		val submission: Id?,
		val reason: String?,
		val at: Instant,
	) {

		/**
		 * The various [Record.Diff] variants.
		 */
		@Serializable
		enum class Type {
			/** See [Record.Diff.Initial]. */
			Initial,

			/** See [Record.Diff.EditInitial]. */
			EditInitial,

			/** See [Record.Diff.EditCurrent]. */
			EditCurrent,

			/** See [Record.Diff.Accept]. */
			Accept,

			/** See [Record.Diff.Refuse]. */
			Refuse,

			/** See [Record.Diff.MoveBack]. */
			MoveBack,
		}
	}

	/**
	 * DTO for [Record.Status].
	 */
	@Serializable
	sealed class Status {
		@Serializable
		object Initial : Status()

		@Serializable
		class Step(val step: Int) : Status()

		@Serializable
		object Refused : Status()
	}

	@Serializable
	class Advance(
		val type: Diff.Type,
		val reason: String? = null,
		val submission: Map<String, String>? = null,
		val toStep: Int? = null,
	)

	@Serializable
	object SearchFailures

	@Serializable
	sealed class NewFailures {
		@Serializable
		object CannotCreateRecordForNonInitialStep : NewFailures()

		@Serializable
		class InvalidSubmission(
			val failures: List<SubmissionDto.ParsingFailures>,
		) : NewFailures()
	}

	@Serializable
	object GetFailures

	@Serializable
	object AdvanceFailures
}

//region Conversions

private fun Record.Status.toDto() = when (this) {
	Record.Status.Initial -> RecordDto.Status.Initial
	Record.Status.Refused -> RecordDto.Status.Refused
	is Record.Status.Step -> RecordDto.Status.Step(step)
}

private fun RecordDto.Status.toCore() = when (this) {
	RecordDto.Status.Initial -> Record.Status.Initial
	RecordDto.Status.Refused -> Record.Status.Refused
	is RecordDto.Status.Step -> Record.Status.Step(step)
}

suspend fun RecordDto.toCore(forms: Form.Service, users: User.Service<*>, submissions: Submission.Service) = Record(
	form = forms.versions.fromIdentifier(api.forms.id.version.identifierOf(form)),
		createdAt = createdAt,
		modifiedAt = modifiedAt,
	history = history.map { it.toCore(users, submissions) },
	)

fun Record.toDto() = RecordDto(
	api.forms.id.version.idOf(form.form.toIdentifier().text, form.creationDate.toString()),
	createdAt,
	modifiedAt,
	status.toDto(),
	history.map { it.toDto() },
)

suspend fun RecordDto.Diff.toCore(users: User.Service<*>, submissions: Submission.Service): Record.Diff {
	val submission = submission?.let { submissions.fromIdentifier(api.submissions.id.identifierOf(it)) }
	val author = author?.let { users.fromIdentifier(api.users.id.identifierOf(it)) }

	return when (type) {
		RecordDto.Diff.Type.Initial -> Record.Diff.Initial(
			submission = submission!!,
			author = author,
			firstStep = (target as RecordDto.Status.Step).step,
			at = at,
		)

		RecordDto.Diff.Type.EditInitial -> Record.Diff.EditInitial(
			submission = submission!!,
			author = author!!,
			reason = reason!!,
			currentStatus = source.toCore() as Record.Status.NonInitial,
			at = at,
		)

		RecordDto.Diff.Type.EditCurrent -> Record.Diff.EditCurrent(
			submission = submission,
			author = author!!,
			currentStatus = source.toCore() as Record.Status.NonInitial,
			reason = reason,
			at = at,
		)

		RecordDto.Diff.Type.Accept -> Record.Diff.Accept(
			submission = submission,
			author = author!!,
			source = source.toCore() as Record.Status.Step,
			target = target.toCore() as Record.Status.Step,
			reason = reason,
			at = at,
		)

		RecordDto.Diff.Type.Refuse -> Record.Diff.Refuse(
			author = author!!,
			source = source.toCore() as Record.Status.Step,
			reason = reason!!,
			at = at,
		)

		RecordDto.Diff.Type.MoveBack -> Record.Diff.MoveBack(
			author = author!!,
			source = source.toCore() as Record.Status.NonInitial,
			target = target.toCore() as Record.Status.Step,
			reason = reason!!,
			at = at,
		)
	}
}

fun Record.Diff.toDto() = RecordDto.Diff(
	author = author?.let { api.users.id.idOf(it.toIdentifier().text) },
	source = source.toDto(),
	target = target.toDto(),
	submission = submission?.let { api.submissions.id.idOf(it.toIdentifier().text) },
	reason = reason,
	at = at,
	type = when (this) {
		is Record.Diff.Accept -> RecordDto.Diff.Type.Accept
		is Record.Diff.EditInitial -> RecordDto.Diff.Type.EditInitial
		is Record.Diff.EditCurrent -> RecordDto.Diff.Type.EditCurrent
		is Record.Diff.Initial -> RecordDto.Diff.Type.Initial
		is Record.Diff.MoveBack -> RecordDto.Diff.Type.MoveBack
		is Record.Diff.Refuse -> RecordDto.Diff.Type.Refuse
	}
)

//endregion
