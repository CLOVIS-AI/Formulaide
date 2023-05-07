package opensavvy.formulaide.fake

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import opensavvy.backbone.now
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.User.Role.Companion.role
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.arrow.out
import opensavvy.state.arrow.toEither
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.onFailure
import opensavvy.state.outcome.onSuccess
import opensavvy.state.progressive.failed
import opensavvy.state.progressive.withProgress

class FakeFiles(
	private val clock: Clock,
) : File.Service {

	private val lock = Mutex()
	private val data = HashMap<Long, File>()
	private val contents = HashMap<Long, ByteArray>()

	override suspend fun create(mime: String, content: ByteIterator): Outcome<File.Failures.Create, File.Ref> = out {
		val id = newId()

		lock.withLock("create") {
			data[id] = File(
				origin = null,
				mime = mime,
				uploadedAt = clock.now(),
			)

			contents[id] = content.asSequence().toList().toByteArray()
		}

		Ref(id)
	}

	override fun fromId(id: String) = Ref(id.toLong())

	inner class Ref internal constructor(
		val realId: Long,
	) : File.Ref {
		override val id: String
			get() = realId.toString()

		override suspend fun linkTo(record: Record.Ref, submission: Submission.Ref, field: Field.Id): Outcome<File.Failures.Link, Unit> = out {
			lock.withLock("linkTo") {
				val file = data[realId]
				ensureNotNull(file) { File.Failures.NotFound(this@Ref) }
				ensure(file.origin == null) { File.Failures.AlreadyLinked(this@Ref) }

				val updated = file.copy(
					origin = File.Origin(
						form = record.now().toEither().mapLeft { File.Failures.RecordNotFound(record, it) }.bind().form.form,
						record = record,
						submission = submission,
						field = field,
					)
				)

				data[realId] = updated
			}
		}

		override suspend fun read(): Outcome<File.Failures.Read, ByteIterator> = out {
			val file = lock.withLock("read:initial") { data[realId] }
			ensureNotNull(file) { File.Failures.NotFound(this@Ref) }
			ensure(
				file.origin == null && currentRole() > User.Role.Employee ||
					file.origin != null && currentRole() >= User.Role.Employee
			) { File.Failures.NotFound(this@Ref) }

			val origin = file.origin
			val timeToLive = if (origin == null) {
				File.TTL_UNLINKED
			} else {
				val submission = origin.submission.now().toEither()
					.mapLeft { File.Failures.SubmissionNotFound(origin.submission, it) }
					.bind()

				val form = submission.form.now().toEither()
					.mapLeft { File.Failures.FormVersionNotFound(submission.form, it) }
					.bind()

				val field = form.findFieldForStep(submission.formStep)
				ensure(field is Field.Input) { File.Failures.InvalidField("The uploaded file ${this@Ref} is invalid: it refers to a field which is not an input: $field") }

				val input = field.input
				ensure(input is Input.Upload) { File.Failures.InvalidField("The uploaded file ${this@Ref} is invalid: it refers to a field which is not an upload input: $input") }

				input.effectiveExpiresAfter
			}

			ensure(clock.now() < file.uploadedAt + timeToLive) {
				lock.withLock("read:remove") { data.remove(realId) }
				File.Failures.Expired(this@Ref)
			}

			val content = lock.withLock("read:read") { contents[realId] }
			ensureNotNull(content) { File.Failures.NotFound(this@Ref) }

			content.iterator()
		}

		override fun request(): ProgressiveFlow<File.Failures.Get, File> = flow {
			val userRef = currentUser() ?: run {
				emit(File.Failures.Unauthenticated.failed())
				return@flow
			}

			val user = userRef.now()

			user.onFailure {
				emit(File.Failures.Unauthenticated.failed())
				return@flow
			}

			user.onSuccess {
				out<File.Failures.Get, File> {
					ensure(it.role >= User.Role.Employee) { File.Failures.Unauthenticated }

					val file = lock.withLock("request") { data[realId] }
					ensureNotNull(file) { File.Failures.NotFound(this@Ref) }

					file
				}.also { emit(it.withProgress()) }
			}
		}

	}
}
