package opensavvy.formulaide.remote.client

import arrow.core.toNonEmptyListOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import opensavvy.cache.contextual.cache
import opensavvy.cache.contextual.cachedInMemory
import opensavvy.cache.contextual.expireAfter
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.RecordDto
import opensavvy.formulaide.remote.dto.SubmissionDto
import opensavvy.formulaide.remote.dto.toCore
import opensavvy.formulaide.remote.dto.toDto
import opensavvy.formulaide.remote.failures.message
import opensavvy.logger.Logger.Companion.warn
import opensavvy.logger.loggerFor
import opensavvy.spine.Parameters
import opensavvy.spine.SpineFailure
import opensavvy.spine.ktor.client.request
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.mapFailure
import kotlin.time.Duration.Companion.minutes

class RemoteRecords(
	private val client: Client,
	private val forms: Form.Service,
	private val users: User.Service<*>,
	scope: CoroutineScope,
) : Record.Service {

	private val log = loggerFor(this)

	private val submissions = Submissions(scope)

	private val cache = cache<Ref, User.Ref?, Record.Failures.Get, Record> { ref, user ->
		out {
			client.http.request(
				api.records.id.get,
				api.records.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Record.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Record.Failures.Unauthorized
					SpineFailure.Type.NotFound -> Record.Failures.NotFound(ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()
				.toCore(forms, users, submissions)
		}
	}.cachedInMemory(scope.coroutineContext.job)
		.expireAfter(5.minutes, scope)

	override suspend fun search(criteria: List<Record.QueryCriterion>): Outcome<Record.Failures.Search, List<Record.Ref>> = out {
		client.http.request(
			api.records.search,
			api.records.idOf(),
			emptyList(),
			Parameters.Empty,
			Unit,
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.Unauthenticated -> Record.Failures.Unauthenticated
				SpineFailure.Type.Unauthorized -> Record.Failures.Unauthorized
				else -> error("Received an unexpected status: $it")
			}
		}.bind()
			.map { fromIdentifier(api.records.id.identifierOf(it)) }
	}

	override suspend fun create(submission: Submission): Outcome<Record.Failures.Create, Record.Ref> = out {
		val (id, newDto) = client.http.request(
			api.records.create,
			api.records.idOf(),
			submission.toDto(),
			Parameters.Empty,
			Unit,
		).mapFailure {
			when (it.type) {
				SpineFailure.Type.Unauthenticated -> Record.Failures.Unauthenticated
				SpineFailure.Type.NotFound -> when (val message = it.message) {
					null -> error("Received a status with no message: $it")
					else -> Record.Failures.FormNotFound(forms.fromIdentifier(message), null)
				}

				SpineFailure.Type.InvalidRequest -> when (val payload = it.payload) {
					is RecordDto.NewFailures.CannotCreateRecordForNonInitialStep -> Record.Failures.CannotCreateRecordForNonInitialStep
					is RecordDto.NewFailures.InvalidSubmission -> Record.Failures.InvalidSubmission(payload.failures.map(SubmissionDto.ParsingFailures::toCore).toNonEmptyListOrNull()!!)
					null -> error("Received a status with no message: $it")
				}
				else -> error("Received an unexpected status: $it")
			}
		}.bind()

		val new = newDto.toCore(forms)
		if (submission != new) {
			log.warn(
				submission,
				new
			) { "The created and received submissions are different, most likely the format is incorrect" }
		}

		fromIdentifier(api.records.id.identifierOf(id))
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

	inner class Ref internal constructor(
		internal val id: String,
	) : Record.Ref {
		override suspend fun editInitial(reason: String, submission: Map<Field.Id, String>): Outcome<Record.Failures.Action, Unit> =
			client.http.request(
				api.records.id.advance,
				api.records.id.idOf(id),
				RecordDto.Advance(
					type = RecordDto.Diff.Type.EditInitial,
					reason = reason,
					submission = submission.mapKeys { (it, _) -> it.toString() },
				),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Record.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Record.Failures.Unauthorized
					else -> error("Received an unexpected status: $it")
				}
			}

		override suspend fun editCurrent(reason: String?, submission: Map<Field.Id, String>): Outcome<Record.Failures.Action, Unit> =
			client.http.request(
				api.records.id.advance,
				api.records.id.idOf(id),
				RecordDto.Advance(
					type = RecordDto.Diff.Type.EditCurrent,
					reason = reason,
					submission = submission.mapKeys { (it, _) -> it.toString() },
				),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Record.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Record.Failures.Unauthorized
					else -> error("Received an unexpected status: $it")
				}
			}

		override suspend fun accept(reason: String?, submission: Map<Field.Id, String>?): Outcome<Record.Failures.Action, Unit> =
			client.http.request(
				api.records.id.advance,
				api.records.id.idOf(id),
				RecordDto.Advance(
					type = RecordDto.Diff.Type.Accept,
					reason = reason,
					submission = submission?.mapKeys { (it, _) -> it.toString() },
				),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Record.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Record.Failures.Unauthorized
					else -> error("Received an unexpected status: $it")
				}
			}

		override suspend fun refuse(reason: String): Outcome<Record.Failures.Action, Unit> =
			client.http.request(
				api.records.id.advance,
				api.records.id.idOf(id),
				RecordDto.Advance(
					type = RecordDto.Diff.Type.Refuse,
					reason = reason,
				),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Record.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Record.Failures.Unauthorized
					else -> error("Received an unexpected status: $it")
				}
			}

		override suspend fun moveBack(toStep: Int, reason: String): Outcome<Record.Failures.Action, Unit> =
			client.http.request(
				api.records.id.advance,
				api.records.id.idOf(id),
				RecordDto.Advance(
					type = RecordDto.Diff.Type.MoveBack,
					reason = reason,
					toStep = toStep,
				),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Record.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Record.Failures.Unauthorized
					else -> error("Received an unexpected status: $it")
				}
			}

		override fun request(): ProgressiveFlow<Record.Failures.Get, Record> = flow {
			emitAll(cache[this@Ref, currentUser()])
		}

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Ref) return false

			return id == other.id
		}

		override fun hashCode(): Int {
			return id.hashCode()
		}

		override fun toString() = "RemoteRecords.Ref($id)"

		override fun toIdentifier() = Identifier(id)

		// endregion

	}

	inner class Submissions(scope: CoroutineScope) : Submission.Service {
		private val cache = cache<Ref, User.Ref?, Submission.Failures.Get, Submission> { ref, user ->
			out {
				client.http.request(
					api.submissions.id.get,
					api.submissions.id.idOf(ref.id),
					Unit,
					Parameters.Empty,
					Unit,
				).mapFailure {
					when (it.type) {
						SpineFailure.Type.Unauthenticated -> Submission.Failures.Unauthenticated
						SpineFailure.Type.Unauthorized -> Submission.Failures.Unauthorized
						SpineFailure.Type.NotFound -> Submission.Failures.NotFound(ref)
						else -> error("Received an unexpected status: $it")
					}
				}.bind()
					.toCore(forms)
			}
		}.cachedInMemory(scope.coroutineContext.job)
			.expireAfter(15.minutes, scope)

		inner class Ref internal constructor(
			internal val id: String,
		) : Submission.Ref {
			override fun request(): ProgressiveFlow<Submission.Failures.Get, Submission> = flow {
				emitAll(cache[this@Ref, currentUser()])
			}

			// region Overrides

			override fun equals(other: Any?): Boolean {
				if (this === other) return true
				if (other !is Ref) return false

				return id == other.id
			}

			override fun hashCode(): Int {
				return id.hashCode()
			}

			override fun toString() = "RemoteRecords.Submissions.Ref($id)"

			override fun toIdentifier() = Identifier(id)

			// endregion
		}

		override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)
	}
}
