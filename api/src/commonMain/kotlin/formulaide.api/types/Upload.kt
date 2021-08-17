package formulaide.api.types

import formulaide.api.data.Action
import formulaide.api.data.Form
import kotlinx.serialization.Serializable

@Serializable
data class Upload(
	override val id: ReferenceId,
	val data: ByteArray,
	val uploadTimestamp: Long,
	val expirationTimestamp: Long,
	val mime: String,
) : Referencable {

	//region Equals & hashCode
	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is Upload) return false

		if (id != other.id) return false
		if (!data.contentEquals(other.data)) return false
		if (uploadTimestamp != other.uploadTimestamp) return false
		if (expirationTimestamp != other.expirationTimestamp) return false
		if (mime != other.mime) return false

		return true
	}

	override fun hashCode(): Int {
		var result = id.hashCode()
		result = 31 * result + data.contentHashCode()
		result = 31 * result + uploadTimestamp.hashCode()
		result = 31 * result + expirationTimestamp.hashCode()
		result = 31 * result + mime.hashCode()
		return result
	}
	//endregion
}

@Serializable
data class UploadRequest(
	val form: Ref<Form>,
	val root: Ref<Action>?,
	val field: String,
)

@Serializable
data class DownloadRequest(
	val id: String,
)
