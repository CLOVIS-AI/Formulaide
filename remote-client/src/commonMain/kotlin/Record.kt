package opensavvy.formulaide.remote.client

import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.*
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.RecordDto
import opensavvy.formulaide.remote.dto.toCore
import opensavvy.formulaide.remote.dto.toDto
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

class Records(
	private val client: Client,
	private val forms: Form.Service,
	private val users: User.Service,
	cacheContext: CoroutineContext,
) : Record.Service {

	private val log = loggerFor(this)

	private val submissions = Submissions(cacheContext)

	override val cache: RefCache<Record> = defaultRefCache<Record>()
		.cachedInMemory(cacheContext)
		.expireAfter(5.minutes, cacheContext)

	override suspend fun search(criteria: List<Record.QueryCriterion>): Outcome<List<Record.Ref>> = out {
		client.http.request(
			api.records.search,
			api.records.idOf(),
			emptyList(),
			Parameters.Empty,
			Unit,
		).bind()
			.map { api.records.id.refOf(it, this@Records).bind() }
	}

	override suspend fun create(submission: Submission): Outcome<Record.Ref> = out {
		val (id, newDto) = client.http.request(
			api.records.create,
			api.records.idOf(),
			submission.toDto(),
			Parameters.Empty,
			Unit,
		).bind()

		val new = newDto.toCore(forms).bind()
		if (submission != new) {
			log.warn(
				submission,
				new
			) { "The created and received submissions are different, most likely the format is incorrect" }
		}

		api.records.id.refOf(id, this@Records).bind()
	}

	override suspend fun editInitial(
		record: Record.Ref,
		reason: String,
		submission: Map<Field.Id, String>,
	): Outcome<Unit> = client.http.request(
		api.records.id.advance,
		api.records.id.idOf(record.id),
		RecordDto.Advance(
			type = RecordDto.Diff.Type.EditInitial,
			reason = reason,
			submission = submission.mapKeys { (it, _) -> it.toString() },
		),
		Parameters.Empty,
		Unit,
	)

	override suspend fun accept(
		record: Record.Ref,
		reason: String?,
		submission: Map<Field.Id, String>?,
	): Outcome<Unit> = client.http.request(
		api.records.id.advance,
		api.records.id.idOf(record.id),
		RecordDto.Advance(
			type = RecordDto.Diff.Type.Accept,
			reason = reason,
			submission = submission?.mapKeys { (it, _) -> it.toString() },
		),
		Parameters.Empty,
		Unit,
	)

	override suspend fun editCurrent(
		record: Record.Ref,
		reason: String?,
		submission: Map<Field.Id, String>,
	): Outcome<Unit> = client.http.request(
		api.records.id.advance,
		api.records.id.idOf(record.id),
		RecordDto.Advance(
			type = RecordDto.Diff.Type.EditCurrent,
			reason = reason,
			submission = submission.mapKeys { (it, _) -> it.toString() },
		),
		Parameters.Empty,
		Unit,
	)

	override suspend fun refuse(record: Record.Ref, reason: String): Outcome<Unit> = client.http.request(
		api.records.id.advance,
		api.records.id.idOf(record.id),
		RecordDto.Advance(
			type = RecordDto.Diff.Type.Refuse,
			reason = reason,
		),
		Parameters.Empty,
		Unit,
	)

	override suspend fun moveBack(record: Record.Ref, toStep: Int, reason: String): Outcome<Unit> = client.http.request(
		api.records.id.advance,
		api.records.id.idOf(record.id),
		RecordDto.Advance(
			type = RecordDto.Diff.Type.MoveBack,
			reason = reason,
			toStep = toStep,
		),
		Parameters.Empty,
		Unit,
	)

	override suspend fun directRequest(ref: Ref<Record>): Outcome<Record> = out {
		ensureValid(ref is Record.Ref) { "Expected Record.Ref, found $ref" }

		client.http.request(
			api.records.id.get,
			api.records.id.idOf(ref.id),
			Unit,
			Parameters.Empty,
			Unit,
		).bind()
			.toCore(forms, users, submissions).bind()
	}

	inner class Submissions(cacheContext: CoroutineContext) : Submission.Service {
		override val cache: RefCache<Submission> = defaultRefCache<Submission>()
			.cachedInMemory(cacheContext)
			.expireAfter(15.minutes, cacheContext)

		override suspend fun directRequest(ref: Ref<Submission>): Outcome<Submission> = out {
			ensureValid(ref is Submission.Ref) { "Expected Submission.Ref, found $ref" }

			client.http.request(
				api.submissions.id.get,
				api.submissions.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				Unit,
			).bind()
				.toCore(forms).bind()
		}
	}
}
