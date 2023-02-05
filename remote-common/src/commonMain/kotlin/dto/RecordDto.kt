package opensavvy.formulaide.remote.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.core.User
import opensavvy.formulaide.remote.api
import opensavvy.spine.Id
import opensavvy.state.outcome.out

@Serializable
class RecordDto(
	val form: Id,
	val createdAt: Instant,
	val modifiedAt: Instant,
	val history: List<Diff>,
) {

	@Serializable
	sealed class Criterion

	@Serializable
	class Diff(
		val type: Type,
		val author: Id?,
		val step: Int?,
		val toStep: Int?,
		val submission: Id?,
		val reason: String?,
		val at: Instant,
	) {

		@Serializable
		enum class Type {
			Initial,
			EditInitial,
			Accept,
			Refuse,
			MoveBack,
		}
	}
}

//region Conversions

suspend fun RecordDto.toCore(forms: Form.Service, users: User.Service, submissions: Submission.Service) = out {
	Record(
		form = api.forms.id.version.refOf(form, forms).bind(),
		createdAt = createdAt,
		modifiedAt = modifiedAt,
		history = history.map { it.toCore(users, submissions).bind() },
	)
}

fun Record.toDto() = RecordDto(
	api.forms.id.version.idOf(form.form.id, form.version.toString()),
	createdAt,
	modifiedAt,
	history.map { it.toDto() }
)

suspend fun RecordDto.Diff.toCore(users: User.Service, submissions: Submission.Service) = out {
	val submission = submission?.let { api.submissions.id.refOf(it, submissions).bind() }
	val author = author?.let { api.users.id.refOf(it, users).bind() }

	when (type) {
		RecordDto.Diff.Type.Initial -> Record.Diff.Initial(
			submission = submission!!,
			author = author,
			at = at,
		)

		RecordDto.Diff.Type.EditInitial -> Record.Diff.EditInitial(
			submission = submission!!,
			author = author!!,
			reason = reason!!,
			at = at,
		)

		RecordDto.Diff.Type.Accept -> Record.Diff.Accept(
			submission = submission,
			author = author!!,
			step = step!!,
			reason = reason,
			at = at,
		)

		RecordDto.Diff.Type.Refuse -> Record.Diff.Refuse(
			author = author!!,
			step = step!!,
			reason = reason!!,
			at = at,
		)

		RecordDto.Diff.Type.MoveBack -> Record.Diff.MoveBack(
			author = author!!,
			step = step!!,
			toStep = toStep!!,
			reason = reason!!,
			at = at,
		)
	}
}

fun Record.Diff.toDto() = RecordDto.Diff(
	author = author?.let { api.users.id.idOf(it.id) },
	step = step,
	toStep = (this as? Record.Diff.MoveBack)?.toStep,
	submission = submission?.let { api.submissions.id.idOf(it.id) },
	reason = reason,
	at = at,
	type = when (this) {
		is Record.Diff.Accept -> RecordDto.Diff.Type.Accept
		is Record.Diff.EditInitial -> RecordDto.Diff.Type.EditInitial
		is Record.Diff.Initial -> RecordDto.Diff.Type.Initial
		is Record.Diff.MoveBack -> RecordDto.Diff.Type.MoveBack
		is Record.Diff.Refuse -> RecordDto.Diff.Type.Refuse
	}
)

//endregion
