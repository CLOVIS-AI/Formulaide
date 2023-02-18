package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.RecordDto
import opensavvy.formulaide.remote.dto.toCore
import opensavvy.formulaide.remote.dto.toDto
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route
import opensavvy.state.outcome.ensureValid

fun Routing.records(
	users: User.Service,
	forms: Form.Service,
	submissions: Submission.Service,
	records: Record.Service,
) {
	route(api.submissions.id.get, contextGenerator) {
		val ref = api.submissions.id.refOf(id, submissions).bind()
		val submission = ref.now().bind()

		submission.toDto()
	}

	route(api.records.id.get, contextGenerator) {
		val ref = api.records.id.refOf(id, records).bind()
		val record = ref.now().bind()

		record.toDto()
	}

	route(api.records.create, contextGenerator) {
		val ref = records.create(
			body.toCore(forms).bind()
		).bind()
		val record = ref.now().bind()

		val diff = record.historySorted.first() as Record.Diff.Initial
		Identified(api.records.id.idOf(ref.id), diff.submission.now().bind().toDto())
	}

	route(api.records.search, contextGenerator) {
		records.search(emptyList()).bind()
			.map { api.records.id.idOf(it.id) }
	}

	route(api.records.id.advance, contextGenerator) {
		val record = api.records.id.refOf(id, records).bind()
		val reason = body.reason
		val submission = body.submission?.mapKeys { (it, _) -> Field.Id.fromString(it) }

		when (body.type) {
			RecordDto.Diff.Type.Initial -> {
				error("Il n'est pas possible de créer une saisie initiale en modifiant un dossier")
			}

			RecordDto.Diff.Type.EditInitial -> {
				ensureValid(reason != null) { "Il est obligatoire de fournir une raison pour modifier la saisie initiale" }
				ensureValid(submission != null) { "Il est impossible de modifier la saisie initiale sans fournir de saisie" }
				records.editInitial(record, reason, submission)
			}

			RecordDto.Diff.Type.EditCurrent -> {
				ensureValid(submission != null) { "Il est impossible de modifier l'étape actuelle sans fournir de saisie" }
				records.editCurrent(record, reason, submission)
			}

			RecordDto.Diff.Type.Accept -> {
				records.accept(record, reason, submission)
			}

			RecordDto.Diff.Type.Refuse -> {
				ensureValid(reason != null) { "Il est obligatoire de fournir une raison pour refuser un dossier" }
				records.refuse(record, reason)
			}

			RecordDto.Diff.Type.MoveBack -> {
				val target = body.toStep
				ensureValid(reason != null) { "Il est obligatoire de fournir une raison pour renvoyer un dossier à une étape précédente" }
				ensureValid(target != null) { "Il est obligatoire de désigner une étape de destination" }
				records.moveBack(record, target, reason)
			}
		}
	}
}
