package formulaide.client.files

import io.ktor.http.content.*

/**
 * A file that can be uploaded.
 */
abstract class FileUpload {

	/**
	 * Converts this file to a Ktor [MultiPartData].
	 */
	internal abstract fun toMultipart(name: String): MultipartUpload
}
