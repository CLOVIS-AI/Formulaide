package opensavvy.formulaide.fake

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Ref.Companion.now
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.currentUser
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.Failure
import opensavvy.state.outcome.*

class FakeRecords(
	private val clock: Clock,
	private val files: File.Service,
) : Record.Service {

	private val lock = Semaphore(1)
	private val data = HashMap<String, Record>()

	private val _submissions = FakeSubmissions()
	val submissions: Submission.Service = _submissions

	private fun toRef(id: String) = Record.Ref(id, this)

	override suspend fun search(criteria: List<Record.QueryCriterion>): Outcome<List<Record.Ref>> = out {
		ensureEmployee()

		lock.withPermit {
			data.keys.map { toRef(it) }
		}
	}

	override suspend fun create(submission: Submission): Outcome<Record.Ref> = out {
		if (currentRole() == User.Role.Guest && !submission.form.form.now().bind().public) {
			@Suppress("IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION") // Will be fixed in Arrow 2.0
			failed("Le formulaire demandé est introuvable", Failure.Kind.NotFound).bind()
		}

		ensureValid(submission.formStep == null) { "Il n'est pas possible de créer un dossier pour une autre étape que la saisie initiale, ${submission.formStep} a été demandé" }
		submission.parse(files).bind()

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
					firstStep = submission.form.now().bind().stepsSorted.first().id,
				)
			),
		)

		lock.withPermit {
			data[id] = record
		}

		toRef(id)
			.also { linkFiles(it, submissionRef) }
	}

	private suspend fun createSubmission(
		record: Record.Ref,
		submission: Map<Field.Id, String>,
	) = out {
		val rec = record.now().bind()

		val sub = Submission(
			rec.form,
			(rec.status as Record.Status.Step).step,
			submission,
		)

		lock.withPermit {
			val subId = newId()
			_submissions.data[subId] = sub
			_submissions.toRef(subId)
		}.also { linkFiles(record, it) }
	}

	private suspend fun linkFiles(record: Record.Ref, submission: Submission.Ref) = out {
		val sub = submission.now().bind()
		val form = sub.form.now().bind()
		val field = form.findFieldForStep(sub.formStep)

		field.tree
			.filter { (_, it) -> it is Field.Input && it.input is Input.Upload }
			.map { (id, _) -> id to sub.data[id] }
			.forEach { (id, value) ->
				if (value == null) return@forEach

				val file = File.Ref(value, files)

				file.linkTo(
					record,
					submission,
					id,
				)
			}
	}

	private suspend fun advance(
		record: Record.Ref,
		diff: Record.Diff,
	): Outcome<Unit> = out {
		diff.submission?.also {
			it.now().bind().parse(files)
		}

		run {
			val rec = record.now().bind()
			ensureValid(rec.status == diff.source) { "Une transition doit commencer sur l'état actuel du dossier (${rec.status}), trouvé ${diff.source}" }
		}

		val now = clock.now()

		lock.withPermit {
			val rec = data[record.id]
			ensureFound(rec != null) { "Could not find $record" }

			val result = rec.copy(modifiedAt = now, history = rec.history + diff)
			data[record.id] = result
		}

		record.expire()
	}

	override suspend fun editInitial(
		record: Record.Ref,
		reason: String,
		submission: Map<Field.Id, String>,
	): Outcome<Unit> = out {
		ensureEmployee()

		val sub = createSubmission(
			record,
			submission,
		).bind()

		advance(
			record,
			Record.Diff.EditInitial(
				submission = sub,
				author = currentUser()!!,
				reason = reason,
				currentStatus = record.now().bind().status,
				at = clock.now(),
			)
		)
	}

	override suspend fun editCurrent(
		record: Record.Ref,
		reason: String?,
		submission: Map<Field.Id, String>,
	): Outcome<Unit> = out {
		ensureEmployee()

		val sub = createSubmission(
			record,
			submission,
		).bind()

		advance(
			record,
			Record.Diff.EditCurrent(
				submission = sub,
				author = currentUser()!!,
				currentStatus = record.now().bind().status,
				reason = reason,
				at = clock.now(),
			)
		)
	}

	override suspend fun accept(
		record: Record.Ref,
		reason: String?,
		submission: Map<Field.Id, String>?,
	): Outcome<Unit> = out {
		ensureEmployee()

		val sub = submission?.let {
			createSubmission(record, submission)
		}?.bind()

		val rec = record.now().bind()
		val form = rec.form.now().bind()

		ensureValid(rec.status is Record.Status.Step) { "Il est impossible d'accepter un dossier dans cet état : ${rec.status}" }
		val currentStatus = rec.status as Record.Status.Step

		val nextStep = form.stepsSorted.asSequence()
			.dropWhile { it.id <= currentStatus.step }
			.firstOrNull()
		ensureValid(nextStep != null) { "Il est impossible d'accepter un dossier dans le dernier état de son formulaire : ${rec.status}" }

		advance(
			record,
			Record.Diff.Accept(
				submission = sub,
				author = currentUser()!!,
				source = currentStatus,
				target = Record.Status.Step(nextStep.id),
				reason = reason,
				at = clock.now(),
			)
		)
	}

	override suspend fun refuse(record: Record.Ref, reason: String): Outcome<Unit> = out {
		ensureEmployee()
		ensureValid(reason.isNotBlank()) { "Pour refuser un dossier, il est obligatoire de fournir une raison" }

		val rec = record.now().bind()

		ensureValid(rec.status is Record.Status.Step) { "Il est impossible de refuser un dossier dans cet état : ${rec.status}" }
		val currentStatus = rec.status as Record.Status.Step

		advance(
			record,
			Record.Diff.Refuse(
				author = currentUser()!!,
				source = currentStatus,
				reason = reason,
				at = clock.now(),
			)
		)
	}

	override suspend fun moveBack(record: Record.Ref, toStep: Int, reason: String): Outcome<Unit> = out {
		ensureEmployee()
		ensureValid(reason.isNotBlank()) { "Pour renvoyer un dossier, il est obligatoire de fournir une raison" }

		val rec = record.now().bind()

		val previousSteps = rec.history.asSequence()
			.map { it.source }
			.filterIsInstance<Record.Status.Step>()
			.map { it.step }
		ensureValid(toStep in previousSteps) { "Il n'est pas possible de renvoyer un dossier vers un état dans lequel il n'a jamais été" }

		advance(
			record,
			Record.Diff.MoveBack(
				author = currentUser()!!,
				source = rec.status,
				target = Record.Status.Step(toStep),
				reason = reason,
				at = clock.now(),
			)
		)
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
