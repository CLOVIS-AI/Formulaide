package formulaide.client.files

import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.files.Blob
import org.w3c.files.FileReader
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FileUploadJS(private val file: Blob, private val name: String) : FileUpload() {
	override fun toMultipart(name: String): MultipartUpload = MultipartFile(name, file, name)
}

internal class MultipartFile(
	override val name: String,
	private val file: Blob,
	private val filename: String,
) : MultipartUpload {

	private lateinit var buffer: Uint8Array

	private var loaded = false

	override suspend fun load() {
		buffer = getBytesFromFile(file)
		loaded = true
	}

	override fun applyTo(builder: FormBuilder) = with(builder) {
		require(loaded) { "This file has not been loaded. You should call MultipartFile.load beforehand." }

		println("Adding part '$name' (type ${file.type})")
		appendInput(
			name, Headers.build {
				append(HttpHeaders.ContentDisposition, "filename=${filename}")
				append(HttpHeaders.ContentType, file.type)
			}, size = file.size.toLong()
		) {
			buildPacket {
				repeat(buffer.length) { i ->
					writeByte(buffer[i])
				}
			}
		}
	}
}

private suspend fun getBytesFromFile(file: Blob): Uint8Array = suspendCoroutine { continuation ->
	val reader = FileReader()
	reader.readAsArrayBuffer(file)

	reader.onload = {
		continuation.resume(Uint8Array(reader.result as ArrayBuffer))
	}

	reader.onabort = {
		continuation.resumeWithException(RuntimeException("File reading was aborted: $it"))
	}

	reader.onerror = {
		continuation.resumeWithException(RuntimeException("Error during file reading: $it"))
	}
}
