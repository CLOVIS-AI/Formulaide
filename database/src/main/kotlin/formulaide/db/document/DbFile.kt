package formulaide.db.document

import formulaide.api.fields.SimpleField
import formulaide.db.Database
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.Binary
import org.litote.kmongo.newId
import java.time.Instant
import java.time.Period

@Serializable
data class DbFile(
	val id: String,
	@Contextual val contents: Binary,
	val uploadTimestamp: Long,
	val expirationTimestamp: Long,
)

/**
 * Uploads a file represented by the [contents] array, according to the limits set by the [uploadField].
 */
suspend fun Database.uploadFile(
	contents: ByteArray,
	uploadField: SimpleField.Upload,
): String {
	val file = DbFile(
		id = newId<DbFile>().toString(),
		contents = Binary(contents),
		uploadTimestamp = Instant.now().epochSecond,
		expirationTimestamp = Instant.now()
			.plus(Period.ofDays(uploadField.effectiveExpiresAfterDays)).epochSecond,
	)

	uploads.insertOne(file)

	return file.id
}
