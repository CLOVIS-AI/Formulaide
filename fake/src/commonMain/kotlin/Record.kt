package opensavvy.formulaide.fake

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.Auth.Companion.currentUser
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureFound
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out

class FakeRecords(
	private val clock: Clock,
) : Record.Service {

	private val lock = Semaphore(1)
	private val data = HashMap<String, Record>()

	private val _submissions = FakeSubmissions()
	val submissions: Submission.Service = _submissions

	private fun toRef(id: String) = Record.Ref(id, this)

	override suspend fun search(criteria: List<Record.QueryCriterion>): Outcome<List<Record.Ref>> = out {
		lock.withPermit {
			data.keys.map { toRef(it) }
		}
	}

	override suspend fun create(submission: Submission): Outcome<Record.Ref> = out {
		val id = newId()

		val submissionRef = _submissions.lock.withPermit {
			val subId = newId()
			_submissions.data[subId] = submission
			_submissions.toRef(subId)
		}

		val now = clock.now()

		val record = Record(
			form = submission.form,
			createdAt = now,
			modifiedAt = now,
			history = listOf(
				Record.Diff.Initial(
					submission = submissionRef,
					author = currentUser(),
					at = now,
				)
			),
		)

		lock.withPermit {
			data[id] = record
		}

		toRef(id)
	}

	override suspend fun advance(record: Record.Ref, diff: Record.Diff): Outcome<Unit> {
		TODO("Will be implemented in a future issue")
	}

	override val cache: RefCache<Record> = defaultRefCache()

	override suspend fun directRequest(ref: Ref<Record>): Outcome<Record> = out {
		ensureValid(ref is Record.Ref) { "Invalid reference $ref" }

		val result = lock.withPermit { data[ref.id] }
		ensureFound(result != null) { "Could not find $ref" }

		result
	}

	private inner class FakeSubmissions : Submission.Service {
		override val cache: RefCache<Submission> = defaultRefCache()

		val lock = Semaphore(1)
		val data = HashMap<String, Submission>()

		fun toRef(id: String) = Submission.Ref(id, this)

		override suspend fun directRequest(ref: Ref<Submission>): Outcome<Submission> = out {
			ensureValid(ref is Submission.Ref) { "Invalid reference $ref" }

			val result = lock.withPermit { data[ref.id] }
			ensureFound(result != null) { "Could not find $ref" }

			result
		}

	}
}
