package formulaide.client.files

import formulaide.api.types.Ref
import formulaide.api.types.UploadRequest
import formulaide.client.routes.uploadFile
import formulaide.client.testEmployee

// This is voluntarily not @Test
// Each platform should call this method with an appropriate file
suspend fun multipartTest(file: FileUpload) {
	val client = testEmployee()

	val result = client.uploadFile(
		UploadRequest(Ref("0"), "0"), //TODO
		file,
	)
}
