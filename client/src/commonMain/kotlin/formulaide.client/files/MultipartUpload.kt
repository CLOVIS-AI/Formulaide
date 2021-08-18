package formulaide.client.files

import formulaide.client.Client
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlinx.serialization.encodeToString

internal interface MultipartUpload {
	val name: String

	/**
	 * Loads this part into memory.
	 */
	suspend fun load() {}

	/**
	 * Adds this part to [builder].
	 *
	 * Callers should call [load] beforehand.
	 */
	fun applyTo(builder: FormBuilder)

	companion object {

		/**
		 * Creates a JSON part from [json].
		 */
		internal inline fun <reified T> json(name: String, json: T): MultipartUpload =
			JsonPart(name, Client.jsonSerializer.encodeToString(json))

		/**
		 * Creates a simple text part from [text].
		 */
		internal fun text(name: String, text: String): MultipartUpload = TextPart(name, text)
	}
}

internal class JsonPart(override val name: String, private val json: String) : MultipartUpload {
	override fun applyTo(builder: FormBuilder) {
		buildForm(builder, name, json, "application/json")
	}
}

internal class TextPart(override val name: String, private val text: String) : MultipartUpload {
	override fun applyTo(builder: FormBuilder) {
		buildForm(builder, name, text, "text/plain")
	}
}

private fun buildForm(builder: FormBuilder, name: String, data: String, mime: String) {
	println("Adding part '$name' (type $mime)")
	builder.append(FormPart(name, data, Headers.build {
		append(HttpHeaders.ContentType, mime)
	}))
}
