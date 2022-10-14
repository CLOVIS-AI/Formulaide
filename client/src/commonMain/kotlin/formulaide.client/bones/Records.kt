package formulaide.client.bones

import formulaide.api.bones.ApiNewRecord
import formulaide.api.bones.ApiRecordReview
import formulaide.client.Client
import formulaide.core.User
import formulaide.core.form.Form
import formulaide.core.form.Submission
import formulaide.core.record.Record
import formulaide.core.record.RecordBackbone
import kotlinx.datetime.Instant
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.state

class Records(
	private val client: Client,
	override val cache: RefCache<Record>,
) : RecordBackbone {
	override suspend fun create(form: Form.Ref, version: Instant, user: User.Ref?, submission: Submission): Record.Ref =
		client.post("/api/records", body = ApiNewRecord(form, version, submission))

	override suspend fun editInitial(record: Record.Ref, user: User.Ref, submission: Submission) {
		client.post<String>("/api/records/${record.id}/initial", body = submission)
	}

	override suspend fun review(
		record: Record.Ref,
		user: User.Ref,
		step: Int?,
		decision: Record.Decision,
		reason: String?,
		submission: Submission?,
	) {
		client.post<String>(
			"/api/records/${record.id}/review", body = ApiRecordReview(
				step,
				decision,
				reason,
				submission,
			)
		)
	}

	override suspend fun list(): List<Record.Ref> =
		client.get("/api/records/")

	override fun directRequest(ref: Ref<Record>): State<Record> = state {
		ensureValid(ref is Record.Ref) { "${this@Records} doesn't support the reference $ref" }

		val result: Record = client.get("/api/records/${ref.id}")

		emit(successful(result))
	}
}
