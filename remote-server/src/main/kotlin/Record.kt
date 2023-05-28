package opensavvy.formulaide.remote.server

import arrow.core.raise.ensureNotNull
import io.ktor.server.routing.*
import opensavvy.backbone.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.RecordDto
import opensavvy.formulaide.remote.dto.toCore
import opensavvy.formulaide.remote.dto.toDto
import opensavvy.formulaide.remote.server.utils.invalidRequest
import opensavvy.formulaide.remote.server.utils.notFound
import opensavvy.formulaide.remote.server.utils.unauthenticated
import opensavvy.formulaide.remote.server.utils.unauthorized
import opensavvy.spine.Identified
import opensavvy.spine.SpineFailure
import opensavvy.spine.ktor.server.route
import opensavvy.state.arrow.toEither
import opensavvy.state.outcome.mapFailure

fun Routing.records(
	users: User.Service<*>,
	forms: Form.Service,
	submissions: Submission.Service,
	records: Record.Service,
) {
	route(api.submissions.id.get, contextGenerator) {
		val ref = api.submissions.id.identifierOf(id)
			.let(submissions::fromIdentifier)

		ref.now()
			.mapFailure {
				when (it) {
					is Submission.Failures.NotFound -> notFound(ref)
					Submission.Failures.Unauthenticated -> unauthenticated()
					Submission.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.toDto()
	}

	route(api.records.id.get, contextGenerator) {
		val ref = api.records.id.identifierOf(id)
			.let(records::fromIdentifier)

		ref.now()
			.mapFailure {
				when (it) {
					is Record.Failures.NotFound -> notFound(ref)
					Record.Failures.Unauthenticated -> unauthenticated()
					Record.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.toDto()
	}

	route(api.records.create, contextGenerator) {
		val submission = body.toCore(forms)

		val ref = records.create(
			submission,
		).mapFailure {
			when (it) {
				Record.Failures.CannotCreateRecordForNonInitialStep -> invalidRequest(RecordDto.NewFailures.CannotCreateRecordForNonInitialStep)
				is Record.Failures.FormNotFound -> notFound(submission.form.form)
				is Record.Failures.FormVersionNotFound -> notFound(submission.form)
				is Record.Failures.InvalidSubmission -> invalidRequest(RecordDto.NewFailures.InvalidSubmission(it.failure.map(Submission.ParsingFailure::toDto)))
				Record.Failures.Unauthenticated -> unauthenticated()
			}
		}.toEither()
			.bind()

		val record = ref.now()
			.mapFailure { error("Could not get the record immediately after creating it: $it") }
			.toEither()
			.bind()

		val diff = record.historySorted.first() as Record.Diff.Initial

		val resultSubmission = diff.submission.now()
			.mapFailure { error("Could not get the submission immediately after creating it: $it") }
			.toEither()
			.bind()

		Identified(api.records.id.idOf(ref.toIdentifier().text), resultSubmission.toDto())
	}

	route(api.records.search, contextGenerator) {
		records.search(emptyList())
			.mapFailure {
				when (it) {
					Record.Failures.Unauthenticated -> unauthenticated()
					Record.Failures.Unauthorized -> unauthorized()
				}
			}
			.toEither()
			.bind()
			.map { api.records.id.idOf(it.toIdentifier().text) }
	}

	route(api.records.id.advance, contextGenerator) {
		val record = api.records.id.identifierOf(id)
			.let(records::fromIdentifier)
		val reason = body.reason
		val submission = body.submission?.mapKeys { (it, _) -> Field.Id.fromString(it) }

		when (body.type) {
			RecordDto.Diff.Type.Initial -> {
				error("Il n'est pas possible de créer une saisie initiale en modifiant un dossier")
			}

			RecordDto.Diff.Type.EditInitial -> {
				ensureNotNull(reason) { SpineFailure(SpineFailure.Type.InvalidRequest, "Il est obligatoire de fournir une raison pour modifier la saisie initiale") }
				ensureNotNull(submission) { SpineFailure(SpineFailure.Type.InvalidRequest, "Il est impossible de modifier la saisie initiale sans fournir de saisie") }
				record.editInitial(reason, submission)
			}

			RecordDto.Diff.Type.EditCurrent -> {
				ensureNotNull(submission) { SpineFailure(SpineFailure.Type.InvalidRequest, "Il est impossible de modifier l'étape actuelle sans fournir de saisie") }
				record.editCurrent(reason, submission)
			}

			RecordDto.Diff.Type.Accept -> {
				record.accept(reason, submission)
			}

			RecordDto.Diff.Type.Refuse -> {
				ensureNotNull(reason) { SpineFailure(SpineFailure.Type.InvalidRequest, "Il est obligatoire de fournir une raison pour refuser un dossier") }
				record.refuse(reason)
			}

			RecordDto.Diff.Type.MoveBack -> {
				val target = body.toStep
				ensureNotNull(reason) { SpineFailure(SpineFailure.Type.InvalidRequest, "Il est obligatoire de fournir une raison pour renvoyer un dossier à une étape précédente") }
				ensureNotNull(target) { SpineFailure(SpineFailure.Type.InvalidRequest, "Il est obligatoire de désigner une étape de destination") }
				record.moveBack(target, reason)
			}
		}
	}
}
