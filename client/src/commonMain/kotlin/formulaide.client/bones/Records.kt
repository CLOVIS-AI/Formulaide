package formulaide.client.bones

import formulaide.api.bones.ApiNewRecord
import formulaide.api.bones.ApiRecordReview
import formulaide.client.Client
import formulaide.core.User
import formulaide.core.form.Form
import formulaide.core.form.Submission
import formulaide.core.record.Record
import formulaide.core.record.RecordBackbone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref
import opensavvy.backbone.Result

class Records(
	private val client: Client,
	override val cache: Cache<Record>,
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

	override fun directRequest(ref: Ref<Record>): Flow<Data<Record>> {
		require(ref is Record.Ref) { "$this doesn't support the reference $ref" }

		return flow {
			val result: Record = client.get("/api/records/${ref.id}")
			emit(Data(Result.Success(result), Data.Status.Completed, ref))
		}
	}
}
