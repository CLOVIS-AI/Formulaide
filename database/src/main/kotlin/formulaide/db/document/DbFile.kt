package formulaide.db.document

import formulaide.api.fields.SimpleField
import formulaide.api.types.Upload
import formulaide.db.Database
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.bson.types.Binary
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import java.time.Instant
import java.time.Period

@Serializable
data class DbFile(
	val id: String,
	@Contextual val contents: Binary,
	val uploadTimestamp: Long,
	val expirationTimestamp: Long,
	val mime: String,
)

/**
 * Uploads a file represented by the [contents] array, according to the limits set by the [uploadField].
 */
suspend fun Database.uploadFile(
	contents: ByteArray,
	mime: String,
	uploadField: SimpleField.Upload,
): String {
	val file = DbFile(
		id = newId<DbFile>().toString(),
		contents = Binary(contents),
		uploadTimestamp = Instant.now().epochSecond,
		expirationTimestamp = Instant.now()
			.plus(Period.ofDays(uploadField.effectiveExpiresAfterDays)).epochSecond,
		mime = mime,
	)

	uploads.insertOne(file)

	return file.id
}

suspend fun Database.downloadFile(id: String): DbFile? =
	uploads.findOne(DbFile::id eq id)

fun DbFile.toApi() =
	Upload(id, String(contents.data, Charsets.UTF_8), uploadTimestamp, expirationTimestamp, mime)
