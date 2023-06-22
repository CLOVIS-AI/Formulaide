package opensavvy.formulaide.fake

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import opensavvy.backbone.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.arrow.out
import opensavvy.state.arrow.toEither
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.failureOrNull
import opensavvy.state.progressive.withProgress

class FakeRecords(
	private val clock: Clock,
	private val files: File.Service,
) : Record.Service {

	private val lock = Mutex()
	private val data = HashMap<Long, Record>()

	private val _submissions = FakeSubmissions()
	val submissions = _submissions

	override suspend fun search(criteria: List<Record.QueryCriterion>): Outcome<Record.Failures.Search, List<Record.Ref>> = out {
		ensureEmployee { Record.Failures.Unauthenticated }

		lock.withLock("search") {
			data.keys.map { Ref(it) }
		}
	}

	override suspend fun create(submission: Submission): Outcome<Record.Failures.Create, Record.Ref> = out {
		if (currentRole() == User.Role.Guest) {
			val form = submission.form.form.now()
			ensure(form is Outcome.Success) { Record.Failures.FormNotFound(submission.form.form, form.failureOrNull!!) }
			ensure(form.value.public) { Record.Failures.FormNotFound(submission.form.form, null) }
		}

		ensure(submission.formStep == null) { Record.Failures.CannotCreateRecordForNonInitialStep }
		val parsed = submission.parse(files)
			.mapLeft { Record.Failures.InvalidSubmission(it) }
			.bind()

		val id = newId()

		val submissionRef = _submissions.lock.withLock("create:submission") {
			val subId = newId()
			_submissions.data[subId] = parsed.submission
			_submissions.Ref(subId)
		}

		val now = clock.now()

		val formVersion = parsed.submission.form.now()
			.toEither()
			.mapLeft { Record.Failures.FormVersionNotFound(parsed.submission.form, it) }
			.bind()

		val record = Record(
			form = parsed.submission.form,
			createdAt = now,
			modifiedAt = now,
			history = listOf(
				Record.Diff.Initial(
					submission = submissionRef,
					author = currentUser(),
					at = now,
					firstStep = formVersion.stepsSorted.first().id,
				)
			),
		)

		lock.withLock("create:record") {
			data[id] = record
		}

		Ref(id)
			.also { linkFiles(it, submissionRef) }
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text.toLong())

	private suspend fun linkFiles(record: Record.Ref, submission: Submission.Ref) = out {
		val sub = submission.now().bind()
		val form = sub.form.now().bind()
		val field = form.findFieldForStep(sub.formStep)

		field.tree
			.filter { (_, it) -> it is Field.Input && it.input is Input.Upload }
			.map { (id, _) -> id to sub.data[id] }
			.forEach { (id, value) ->
				if (value == null) return@forEach

				val file = files.fromIdentifier(Identifier(value))

				file.linkTo(
					record,
					submission,
					id,
				)
			}
	}

	inner class Ref(
		private val id: Long,
	) : Record.Ref {
		private suspend fun createSubmission(
			submission: Map<Field.Id, String>,
		) = out {
			val rec = now().bind()

			val sub = Submission(
				rec.form,
				(rec.status as Record.Status.Step).step,
				submission,
			)

			val subRef = lock.withLock("createSubmission") {
				val subId = newId()
				_submissions.data[subId] = sub
				_submissions.Ref(subId)
			}.also { linkFiles(this@Ref, it) }

			subRef to sub
		}

		private suspend fun advance(
			diff: Record.Diff,
			submission: Submission?,
		): Outcome<Record.Failures.Action, Unit> = out {
			diff.submission?.also {
				requireNotNull(submission) { "If the user created a submission, it should have been created before this point." }
				submission.parse(files)
					.mapLeft { Record.Failures.InvalidSubmission(it) }
					.bind()
			}

			val rec = now().bind()
			ensure(rec.status == diff.source) { Record.Failures.DiffShouldStartAtCurrentState(rec.status, diff.source) }

			val now = clock.now()

			lock.withLock("advance") {
				val result = rec.copy(modifiedAt = now, history = rec.history + diff)
				data[id] = result
			}
		}

		override suspend fun editInitial(reason: String, submission: Map<Field.Id, String>): Outcome<Record.Failures.Action, Unit> = out {
			ensureEmployee { Record.Failures.Unauthenticated }

			val (subRef, sub) = createSubmission(submission).bind()

			advance(
				Record.Diff.EditInitial(
					submission = subRef,
					author = currentUser()!!,
					reason = reason,
					currentStatus = now().bind().status,
					at = clock.now(),
				),
				sub,
			)
		}

		override suspend fun editCurrent(reason: String?, submission: Map<Field.Id, String>): Outcome<Record.Failures.Action, Unit> = out {
			ensureEmployee { Record.Failures.Unauthenticated }

			val (subRef, sub) = createSubmission(submission).bind()

			advance(
				Record.Diff.EditCurrent(
					submission = subRef,
					author = currentUser()!!,
					currentStatus = now().bind().status,
					reason = reason,
					at = clock.now(),
				),
				sub,
			)
		}

		override suspend fun accept(reason: String?, submission: Map<Field.Id, String>?): Outcome<Record.Failures.Action, Unit> = out {
			ensureEmployee { Record.Failures.Unauthenticated }

			val (subRef, sub) = submission
				?.let { createSubmission(it).bind() }
				?: (null to null)

			val rec = now().bind()
			val form = rec.form.now()
				.toEither()
				.mapLeft { Record.Failures.FormVersionNotFound(rec.form, it) }
				.bind()

			ensure(rec.status is Record.Status.Step) { Record.Failures.CannotAcceptRefusedRecord }
			val currentStatus = rec.status as Record.Status.Step

			val nextStep = form.stepsSorted.asSequence()
				.dropWhile { it.id <= currentStatus.step }
				.firstOrNull()
			ensureNotNull(nextStep) { Record.Failures.CannotAcceptFinishedRecord }

			advance(
				Record.Diff.Accept(
					submission = subRef,
					author = currentUser()!!,
					source = currentStatus,
					target = Record.Status.Step(nextStep.id),
					reason = reason,
					at = clock.now(),
				),
				sub,
			)
		}

		override suspend fun refuse(reason: String): Outcome<Record.Failures.Action, Unit> = out {
			ensureEmployee { Record.Failures.Unauthenticated }
			ensure(reason.isNotBlank()) { Record.Failures.MandatoryReason }

			val rec = now().bind()

			ensure(rec.status is Record.Status.Step) { Record.Failures.CannotRefuseRefusedRecord }
			val currentStatus = rec.status as Record.Status.Step

			advance(
				Record.Diff.Refuse(
					author = currentUser()!!,
					source = currentStatus,
					reason = reason,
					at = clock.now(),
				),
				null,
			)
		}

		override suspend fun moveBack(toStep: Int, reason: String): Outcome<Record.Failures.Action, Unit> = out {
			ensureEmployee { Record.Failures.Unauthenticated }
			ensure(reason.isNotBlank()) { Record.Failures.MandatoryReason }

			val rec = now().bind()

			val previousSteps = rec.history.asSequence()
				.map { it.source }
				.filterIsInstance<Record.Status.Step>()
				.map { it.step }
			ensure(toStep in previousSteps) { Record.Failures.CannotMoveBackToFutureStep }

			advance(
				Record.Diff.MoveBack(
					author = currentUser()!!,
					source = rec.status,
					target = Record.Status.Step(toStep),
					reason = reason,
					at = clock.now(),
				),
				null,
			)
		}

		override fun request(): ProgressiveFlow<Record.Failures.Get, Record> = flow {
			out {
				val result = lock.withLock("request") { data[id] }
				ensureNotNull(result) { Record.Failures.NotFound(this@Ref) }

				result
			}.also { emit(it.withProgress()) }
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

		override fun toString() = "FakeRecords.Ref($id)"
		override fun toIdentifier() = Identifier(id.toString())

		// endregion
	}

	inner class FakeSubmissions : Submission.Service {
		val lock = Mutex()
		val data = HashMap<Long, Submission>()

		inner class Ref(
			private val id: Long,
		) : Submission.Ref {
			override fun request(): ProgressiveFlow<Submission.Failures.Get, Submission> = flow {
				out {
					val result = lock.withLock("request") { data[id] }
					ensureNotNull(result) { Submission.Failures.NotFound(this@Ref) }
					result
				}.also { emit(it.withProgress()) }
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

			override fun toString() = "FakeFiles.FakeSubmissions.Ref($id)"
			override fun toIdentifier() = Identifier(id.toString())

			// endregion
		}

		override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text.toLong())
	}
}
