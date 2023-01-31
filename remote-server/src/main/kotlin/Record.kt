package opensavvy.formulaide.remote.server

import io.ktor.server.routing.*
import opensavvy.backbone.Ref.Companion.now
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.core.User
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.toCore
import opensavvy.formulaide.remote.dto.toDto
import opensavvy.spine.Identified
import opensavvy.spine.ktor.server.route

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
		records.advance(
			api.records.id.refOf(id, records).bind(),
			body.toCore(users, submissions).bind()
		)
	}
}
