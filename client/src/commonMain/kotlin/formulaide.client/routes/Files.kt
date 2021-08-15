package formulaide.client.routes

import formulaide.api.types.UploadRequest
import formulaide.client.Client
import formulaide.client.files.FileUpload
import formulaide.client.files.MultipartUpload

/**
 * Uploads a file to the server.
 *
 * - POST /uploads/new
 * - Body: multipart request with a part named 'body' ([UploadRequest]) and a part named 'file'.
 * - Response: the text `Success`
 */
suspend fun Client.uploadFile(request: UploadRequest, file: FileUpload): String =
	postMultipart(
		"/uploads/new",
		MultipartUpload.json("body", request),
		file.toMultipart("file"),
	)
