package opensavvy.formulaide.fake.spies

import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.logger.loggerFor
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.map

class SpyRecords(private val upstream: Record.Service) : Record.Service {

	private val log = loggerFor(upstream)

	override suspend fun search(criteria: List<Record.QueryCriterion>): Outcome<Record.Failures.Search, List<Ref>> = spy(
		log, "search", criteria,
	) { upstream.search(criteria) }
		.map { it.map(::Ref) }

	override suspend fun create(submission: Submission): Outcome<Record.Failures.Create, Ref> = spy(
		log, "create", submission,
	) { upstream.create(submission) }
		.map(::Ref)

	override fun fromIdentifier(identifier: Identifier) = upstream.fromIdentifier(identifier).let(::Ref)

	inner class Ref internal constructor(
		private val upstream: Record.Ref,
	) : Record.Ref {
		override suspend fun editInitial(reason: String, submission: Map<Field.Id, String>): Outcome<Record.Failures.Action, Unit> = spy(
			log, "editInitial", reason, submission,
		) { upstream.editInitial(reason, submission) }

		override suspend fun editCurrent(reason: String?, submission: Map<Field.Id, String>): Outcome<Record.Failures.Action, Unit> = spy(
			log, "editCurrent", reason, submission,
		) { upstream.editCurrent(reason, submission) }

		override suspend fun accept(reason: String?, submission: Map<Field.Id, String>?): Outcome<Record.Failures.Action, Unit> = spy(
			log, "accept", reason, submission,
		) { upstream.accept(reason, submission) }

		override suspend fun refuse(reason: String): Outcome<Record.Failures.Action, Unit> = spy(
			log, "refuse", reason,
		) { upstream.refuse(reason) }

		override suspend fun moveBack(toStep: Int, reason: String): Outcome<Record.Failures.Action, Unit> = spy(
			log, "moveBack", toStep, reason,
		) { upstream.moveBack(toStep, reason) }

		override fun request(): ProgressiveFlow<Record.Failures.Get, Record> = spy(
			log, "request",
		) { upstream.request() }

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Ref) return false

			return upstream == other.upstream
		}

		override fun hashCode(): Int {
			return upstream.hashCode()
		}

		override fun toString() = upstream.toString()
		override fun toIdentifier() = upstream.toIdentifier()

		// endregion
	}

	companion object {

		fun Record.Service.spied() = SpyRecords(this)
	}
}
