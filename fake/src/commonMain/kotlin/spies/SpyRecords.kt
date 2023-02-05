package opensavvy.formulaide.fake.spies

import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
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

	override suspend fun advance(record: Record.Ref, diff: Record.Diff): Outcome<Unit> = spy(
		log, "advance", record, diff,
	) { upstream.advance(record, diff) }

	override val cache: RefCache<Record>
		get() = upstream.cache

	override suspend fun directRequest(ref: Ref<Record>): Outcome<Record> = spy(
		log, "directRequest", ref,
	) { upstream.directRequest(ref) }

	companion object {

		fun Record.Service.spied() = SpyRecords(this)
	}
}
