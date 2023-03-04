package opensavvy.formulaide.fake.spies

import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.logger.loggerFor
import opensavvy.state.outcome.Outcome

class SpyRecords(private val upstream: Record.Service) : Record.Service {

	private val log = loggerFor(upstream)

	override suspend fun search(criteria: List<Record.QueryCriterion>): Outcome<List<Record.Ref>> = spy(
		log, "search", criteria,
	) { upstream.search(criteria) }

	override suspend fun create(submission: Submission): Outcome<Record.Ref> = spy(
		log, "create", submission,
	) { upstream.create(submission) }

	override suspend fun editInitial(
		record: Record.Ref,
		reason: String,
		submission: Map<Field.Id, String>,
	): Outcome<Unit> = spy(log, "editInitial", record, reason, submission) {
		upstream.editInitial(record, reason, submission)
	}

	override suspend fun editCurrent(
		record: Record.Ref,
		reason: String?,
		submission: Map<Field.Id, String>,
	): Outcome<Unit> = spy(log, "editCurrent", record, reason, submission) {
		upstream.editCurrent(record, reason, submission)
	}

	override suspend fun accept(
		record: Record.Ref,
		reason: String?,
		submission: Map<Field.Id, String>?,
	): Outcome<Unit> = spy(log, "accept", record, reason, submission) {
		upstream.accept(record, reason, submission)
	}

	override suspend fun refuse(record: Record.Ref, reason: String): Outcome<Unit> =
		spy(log, "refuse", record, reason) {
			upstream.refuse(record, reason)
		}

	override suspend fun moveBack(record: Record.Ref, toStep: Int, reason: String): Outcome<Unit> =
		spy(log, "moveBack", record, toStep, reason) {
			upstream.moveBack(record, toStep, reason)
		}

	override val cache: RefCache<Record>
		get() = upstream.cache

	override suspend fun directRequest(ref: Ref<Record>): Outcome<Record> = spy(
		log, "directRequest", ref,
	) { upstream.directRequest(ref) }

	companion object {

		fun Record.Service.spied() = SpyRecords(this)
	}
}
