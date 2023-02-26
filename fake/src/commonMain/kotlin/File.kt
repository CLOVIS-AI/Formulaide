package opensavvy.formulaide.fake

import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.File
import opensavvy.formulaide.core.Record
import opensavvy.formulaide.core.Submission
import opensavvy.state.outcome.Outcome

class FakeFiles(
	val clock: Clock,
) : File.Service {

	override val cache: RefCache<File> = defaultRefCache()

	private val lock = Mutex()
	private val data = HashMap<String, File>()

	override suspend fun create(mime: String, content: ByteIterator): Outcome<File.Ref> {
		TODO("Not yet implemented")
	}

	override suspend fun link(
		upload: File.Ref,
		record: Record.Ref,
		submission: Submission.Ref,
		field: Field.Id,
	): Outcome<Unit> {
		TODO("Not yet implemented")
	}

	override suspend fun read(upload: File.Ref): Outcome<ByteIterator> {
		TODO("Not yet implemented")
	}

	override suspend fun directRequest(ref: Ref<File>): Outcome<File> {
		TODO("Not yet implemented")
	}

}
