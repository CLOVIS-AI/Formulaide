package formulaide.server.routes

import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.fields.SimpleField.Upload.Format.Companion.allowsContentType
import formulaide.api.types.DownloadRequest
import formulaide.api.types.Ref
import formulaide.api.types.Upload
import formulaide.api.types.UploadRequest
import formulaide.db.document.downloadFile
import formulaide.db.document.findForm
import formulaide.db.document.toApi
import formulaide.db.document.uploadFile
import formulaide.server.Auth
import formulaide.server.database
import formulaide.server.serializer
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import org.apache.tika.config.TikaConfig
import java.io.InputStream
import org.apache.tika.metadata.Metadata as TikaMetadata

fun Routing.fileRoutes() {
	route("/uploads") {

		authenticate(Auth.Employee) {
			post("/get") {
				val body = call.receive<DownloadRequest>()
				val file = database.downloadFile(body.id)
					?: error("Le fichier ${body.id} est introuvable.")
				call.respond(file.toApi())
			}
		}

		post("/new") {
			// retrieve all multipart data (suspending)
			val multipart = call.receiveMultipart()
			val parts = multipart.readAllParts()
			require(parts.size == 2) { "Une requête pour publier un fichier doit contenir deux parties, trouvé ${parts.size} parties." }

			val body = parts.find { it.name == "body" }
				?: error("Une requête pour publier un fichier doit contenir une partie nommée 'body', trouvé ${parts.map { it.name }}.")
			val file = parts.find { it.name == "file" }
				?: error("Une requête pour publier un fichier doit contenir une partie nommée 'file', trouvé ${parts.map { it.name }}.")

			require(body is PartData.FormItem) { "La partie 'body' devrait être de type binaire, trouvé ${body::class}" }
			require(file is PartData.FileItem) { "La partie 'file' devrait être de type fichier, trouvé ${file::class}" }

			val request = serializer.decodeFromString<UploadRequest>(body.value)

			call.respond(uploadFile(request, file) ?: error("Aucun fichier n'a été envoyé."))

			parts.forEach { it.dispose() }
		}

	}
}

internal suspend fun uploadFile(request: UploadRequest, file: PartData.FileItem): Ref<Upload>? {
	val form = database.findForm(request.form.id)
		?: error("Le formulaire ${request.form} est introuvable, il n'est donc pas possible d'y publier une pièce jointe.")
	val formRoot = request.root
		?.let { root ->
			form.actions.find { it.id == root.id }
				?: error("Le formulaire ${form.id} n'a pas d'étape $root.")
		}
		?.let {
			it.fields
				?: error("L'étape ${it.id} du formulaire ${form.id} n'a pas de champs.")
		}
		?: form.mainFields
	val key = request.field.split(':')
	require(key.isNotEmpty()) { "La clef d'un champ ne peut pas être vide : trouvé '${request.field}'" }

	var field: FormField = formRoot.fields.find { it.id == key[0] }
		?: error("La racine ${request.root} du formulaire ${form.id} n'a pas de champ nommé ${key[0]} au premier niveau.")
	for (id in key.drop(1)) {
		field = when (field) {
			is FormField.Simple -> error("Il est impossible de trouver un sous-champ '$id' dans le champ simple $field")
			is FormField.Union<*> -> field.options.find { it.id == id }
				?: error("Cette union ne contient pas d'option ayant comme identifiant '$id' : $field")
			is FormField.Composite -> field.fields.find { it.id == id }
				?: error("Cette donnée ne contient pas de champ ayant comme identifiant '$id' : $field")
		}
	}
	require(field is FormField.Simple) { "Il n'est autorisé de publier un fichier que si le champ déclaré est de type ${FormField.Simple::class}, trouvé ${field::class}" }
	val simple = field.simple
	require(simple is SimpleField.Upload) { "Il n'est autorisé de publier un fichier que si le champ déclaré est de type ${SimpleField.Upload::class}, trouvé ${field.simple::class}" }

	if (file.contentType == ContentType.Application.OctetStream) {
		System.err.println("L'utilisateur a envoyé un fichier de type octet-stream. Il a été ignoré.")
		return null
	}

	var mime = file.contentType.toString()
	require(simple.allowedFormats.allowsContentType(mime)) { "Le type de fichier '$mime' ne correspond à aucun type autorisé pour ce champ : ${simple.allowedFormats.flatMap { it.mimeTypes }}" }

	@Suppress("BlockingMethodInNonBlockingContext")
	val bytes = withContext(Dispatchers.IO) {
		file.streamProvider().buffered().use {
			mime = getFileType(it, file.originalFileName, mime)
			require(simple.allowedFormats.allowsContentType(mime)) { "L'analyse du fichier a donné comme type '$mime', qui ne correspond à aucun type autorisé pour ce champ : ${simple.allowedFormats.flatMap { it.mimeTypes }}" }

			// Loads the file into memory, so it can be sent to the database.
			// If the file is too big, the file is truncated to the max size.
			val bytes = it.readNBytes(simple.effectiveMaxSizeMB * 1024 * 1024)
			// If we stopped here, the user would not be warned about files that are too big,
			// and the truncated file would be stored in the BD. In turn, this would let employees open truncated files, which are probably corrupted.
			// To stop this scenario, we attempt to read a few more bytes. If the read succeeds, we know that the file was too big, and we can fail the request.
			// We assume that this small number of bytes is insignificant towards the server performance, and cannot be used for DoS attacks.
			val future = it.readNBytes(10)
			require(future.isEmpty()) { "Le fichier ne peut pas être plus gros que ${simple.effectiveMaxSizeMB} Mo, mais il fait au moins ${future.size} octets de trop." }

			bytes
		}
	}

	return Ref(database.uploadFile(bytes, mime, simple))
}

private fun getFileType(file: InputStream, fileName: String?, advertisedMime: String?): String {
	val tika = TikaConfig()

	val metadata = TikaMetadata().apply {
		advertisedMime?.let { set(TikaMetadata.CONTENT_TYPE, it) }
		fileName?.let { set(TikaMetadata.CONTENT_DISPOSITION, it) }
	}

	return tika.detector.detect(file, metadata).toString()
}
