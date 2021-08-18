package formulaide.client.files

import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import java.io.File
import java.net.URLConnection

class FileUploadJVM(private val file: File) : FileUpload() {
	override fun toMultipart(name: String) = MultipartFile(name, file)
}

internal class MultipartFile(override val name: String, private val file: File) : MultipartUpload {

	override fun applyTo(builder: FormBuilder) {
		val mime: String = file.inputStream().buffered().use {
			URLConnection.guessContentTypeFromStream(it)
		} ?: "application/octet-stream"

		println("Adding part '$name' (type $mime)")
		builder.appendInput(
			key = name,
			headers = Headers.build {
				append(HttpHeaders.ContentDisposition, "filename=${file.name}")
				append(HttpHeaders.ContentType, mime)
			},
			size = file.length()
		) {
			buildPacket {
				writeFully(file.readBytes())
			}
		}
	}
}
