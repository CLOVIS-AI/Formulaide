package formulaide.client.routes

import formulaide.api.types.DownloadRequest
import formulaide.api.types.Ref
import formulaide.api.types.Upload
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
suspend fun Client.uploadFile(request: UploadRequest, file: FileUpload): Ref<Upload> =
	postMultipart(
		"/uploads/new",
		MultipartUpload.json("body", request),
		file.toMultipart("file"),
	)

/**
 * Downloads a specific file.
 *
 * - POST /uploads/get
 * - Body: [DownloadRequest]
 * - Response: [Upload]
 */
suspend fun Client.Authenticated.downloadFile(id: String): Upload =
	post("/uploads/get", DownloadRequest(id))
