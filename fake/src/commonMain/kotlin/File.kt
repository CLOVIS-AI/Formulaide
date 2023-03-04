package opensavvy.formulaide.fake

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.now
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureFound
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out

class FakeFiles(
	private val clock: Clock,
) : File.Service {

	override val cache: RefCache<File> = defaultRefCache()

	private val lock = Mutex()
	private val data = HashMap<String, File>()
	private val contents = HashMap<String, ByteArray>()

	override suspend fun create(mime: String, content: ByteIterator): Outcome<File.Ref> = out {
		val id = newId()

		lock.withLock {
			data[id] = File(
				origin = null,
				mime = mime,
				uploadedAt = clock.now(),
			)

			contents[id] = content.asSequence().toList().toByteArray()
		}

		File.Ref(id, this@FakeFiles)
	}

	override suspend fun link(
		upload: File.Ref,
		record: Record.Ref,
		submission: Submission.Ref,
		field: Field.Id,
	): Outcome<Unit> = out {
		lock.withLock {
			val file = data[upload.id]
			ensureFound(file != null) { "Coud not find $file" }
			ensureValid(file.origin == null) { "This file has already been linked to ${file.origin}" }

			val updated = file.copy(
				origin = File.Origin(
					record.now().bind().form.form,
					record,
					submission,
					field,
				)
			)

			data[upload.id] = updated
		}
	}

	override suspend fun read(upload: File.Ref): Outcome<ByteIterator> = out {
		val id = upload.id

		val file = lock.withLock { data[id] }
		ensureFound(file != null) { "Could not find $upload" }
		ensureFound(
			file.origin == null && currentRole() > User.Role.Employee ||
					file.origin != null && currentRole() >= User.Role.Employee
		) { "Could not find $upload" }

		val origin = file.origin
		val timeToLive = if (origin == null) {
			File.TTL_UNLINKED
		} else {
			val submission = origin.submission.now().bind()

			val form = submission.form.now().bind()
			val field = form.findFieldForStep(submission.formStep)
			ensureValid(field is Field.Input) { "The uploaded file $upload is invalid: it refers to a field which is not an input: $field" }

			val input = field.input
			ensureValid(input is Input.Upload) { "The uploaded file $upload is invalid: it refers to a field which is not an upload input: $input" }

			input.effectiveExpiresAfter
		}

		ensureFound(clock.now() < file.uploadedAt + timeToLive) {
			lock.withLock { data.remove(id) }
			"Could not find $upload"
		}

		val content = lock.withLock { contents[id] }
		ensureFound(content != null) { "Could not find $upload" }

		content.iterator()
	}

	override suspend fun directRequest(ref: Ref<File>): Outcome<File> = out {
		ensureValid(ref is File.Ref) { "Invalid reference $ref" }
		ensureEmployee()

		val file = lock.withLock { data[ref.id] }
		ensureFound(file != null) { "Could not find $ref" }

		file
	}

}
