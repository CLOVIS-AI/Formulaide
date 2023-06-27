package opensavvy.formulaide.core

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import opensavvy.backbone.now
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.state.arrow.out
import opensavvy.state.arrow.toEither
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.failed
import opensavvy.state.outcome.success
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import opensavvy.formulaide.core.data.Email as DataEmail

/**
 * A simple piece of information requested of the user filling in the form.
 */
sealed class Input {

	abstract suspend fun parse(value: String, uploads: File.Service): Outcome<Failures.Parsing, Any>

	/**
	 * An input is compatible with its [source] if it is more restrictive (or equivalent).
	 */
	abstract suspend fun validateCompatibleWith(source: Input): Outcome<Failures.Compatibility, Unit>

	data class Text(
		val maxLength: UInt?,
	) : Input() {
		val effectiveMaxLength get() = maxLength ?: 4096u

		override suspend fun parse(value: String, uploads: File.Service) = out {
			ensure(value.length <= effectiveMaxLength.toInt()) { Failures.Parsing("Le texte saisi fait plus de $effectiveMaxLength caractères : ${value.length}") }
			value
		}

		override suspend fun validateCompatibleWith(source: Input) = out {
			ensure(source is Text) { Failures.Compatibility("Impossible d'importer un texte à partir d'un ${source::class} (${this@Text} -> $source)") }

			ensure(effectiveMaxLength <= source.effectiveMaxLength) { Failures.Compatibility("Un texte importé ne peut pas autoriser une longueur ($effectiveMaxLength) plus élevée que celle de sa source (${source.effectiveMaxLength})") }
		}

		override fun toString() = "Texte (longueur maximale : $effectiveMaxLength)"
	}

	data class Integer(
		val min: Long?,
		val max: Long?,
	) : Input() {
		val effectiveMin get() = min ?: Long.MIN_VALUE
		val effectiveMax get() = max ?: Long.MAX_VALUE

		init {
			require(effectiveMin < effectiveMax) { "La valeur minimale ($effectiveMin) doit être inférieure à la valeur maximale ($effectiveMax)" }
		}

		constructor(range: IntRange) : this(range.first.toLong(), range.last.toLong())
		constructor(range: LongRange) : this(range.first, range.last)

		val effectiveRange get() = effectiveMin..effectiveMax

		override suspend fun parse(value: String, uploads: File.Service) = out {
			val long = value.toLongOrNull()
			ensure(long != null) { Failures.Parsing("'$value' n'est pas un nombre valide") }

			ensure(long >= effectiveMin) { Failures.Parsing("$value est inférieur à la valeur minimale autorisée, $effectiveMin") }
			ensure(long <= effectiveMax) { Failures.Parsing("$value est supérieur à la valeur maximale autorisée, $effectiveMax") }

			long
		}

		override suspend fun validateCompatibleWith(source: Input) = out {
			ensure(source is Integer) { Failures.Compatibility("Impossible d'importer un texte à partir d'un ${source::class} (${this@Integer} -> $source)") }

			ensure(effectiveMin >= source.effectiveMin) { Failures.Compatibility("Un nombre importé ne peut pas autoriser une valeur minimale ($effectiveMin) plus petite que celle de sa source (${source.effectiveMin})") }
			ensure(effectiveMax <= source.effectiveMax) { Failures.Compatibility("Un nombre importé ne peut pas autoriser une valeur maximale ($effectiveMax) plus grande que celle de sa source (${source.effectiveMax})") }
		}

		override fun toString() = "Nombre (de $effectiveMin à $effectiveMax)"
	}

	object Toggle : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			val bool = value.toBooleanStrictOrNull()
			ensure(bool != null) { Failures.Parsing("'$value' n'est pas un booléen") }

			bool
		}

		override suspend fun validateCompatibleWith(source: Input) = out {
			ensure(source is Toggle) { Failures.Compatibility("Impossible d'importer un booléen à partir d'un ${source::class} (${this@Toggle} -> $source)") }
		}

		override fun toString() = "Coche"
	}

	object Email : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			captureParsingFailure { DataEmail(value) }
		}

		override suspend fun validateCompatibleWith(source: Input) = out {
			ensure(source is Email) { Failures.Compatibility("Impossible d'importer une adresse email à partir d'un ${source::class} (${this@Email} -> $source)") }
		}

		override fun toString() = "Adresse électronique"
	}

	object Phone : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			ensure(value.length <= 20) { Failures.Parsing("Un numéro de téléphone ne peut pas comporter ${value.length} caractères") }

			for (char in value) {
				ensure(char.isDigit() || char == '+') { Failures.Parsing("Le caractère '$char' n'est pas autorisé dans un numéro de téléphone") }
			}

			value
		}

		override suspend fun validateCompatibleWith(source: Input) = out {
			ensure(source is Phone) { Failures.Compatibility("Impossible d'importer un numéro de téléphone à partir d'un ${source::class} (${this@Phone} -> $source)") }
		}

		override fun toString() = "Numéro de téléphone"
	}

	object Date : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			captureParsingFailure { LocalDate.parse(value) }
		}

		override suspend fun validateCompatibleWith(source: Input) = out {
			ensure(source is Date) { Failures.Compatibility("Impossible d'importer une date à partir d'un ${source::class} (${this@Date} -> $source)") }
		}

		override fun toString() = "Date"
	}

	object Time : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			captureParsingFailure { LocalTime.parse(value) }
		}

		override suspend fun validateCompatibleWith(source: Input) = out {
			ensure(source is Time) { Failures.Compatibility("Impossible d'importer une heure à partir d'un ${source::class} (${this@Time} -> $source)") }
		}

		override fun toString() = "Heure"
	}

	data class Upload(
		val allowedFormats: Set<Format>,
		val maxSizeMB: Int?,
		val expiresAfter: Duration?,
	) : Input() {

		val effectiveMaxSizeMB get() = maxSizeMB ?: maxAllowedSizeMB
		val effectiveExpiresAfter get() = expiresAfter ?: defaultExpiresAfter

		init {
			require(effectiveMaxSizeMB > 0) { "Il n'est pas possible d'exiger des pièces jointes de taille 0" }
			require(effectiveMaxSizeMB <= maxAllowedSizeMB) { "Une pièce jointe ne peut pas faire plus de $maxAllowedSizeMB Mo" }

			require(effectiveExpiresAfter < (31 * 12 * 10).days) { "Une pièce jointe ne peux pas être stockée plus de 10 ans" }
		}

		override suspend fun parse(value: String, uploads: File.Service): Outcome<Failures.Parsing, Any> = out {
			ensure(value.isNotBlank()) { Failures.Parsing("Un identifiant de fichier ne peut pas être vide : '$value'") }

			val file = uploads.fromIdentifier(Identifier(value))

			// Check that it exists
			file.now().toEither().mapLeft { Failures.Parsing("Le fichier est introuvable", cause = it) }.bind()

			file
		}

		override suspend fun validateCompatibleWith(source: Input) = out {
			ensure(source is Upload) { Failures.Compatibility("Impossible d'importer une heure à partir d'un ${source::class} (${this@Upload} -> $source)") }

			ensure(effectiveMaxSizeMB <= source.effectiveMaxSizeMB) { Failures.Compatibility("Un fichier importé ne peut pas autoriser une taille maximale ($effectiveMaxSizeMB Mo) plus grande que celle de sa source (${source.effectiveMaxSizeMB})") }
			ensure(effectiveExpiresAfter <= source.effectiveExpiresAfter) { Failures.Compatibility("Un fichier importé ne peut pas être conservé (${effectiveExpiresAfter.inWholeDays} jours) plus grande que celle de sa source (${source.effectiveExpiresAfter} jours)") }
			ensure(allowedFormats.all { it in source.allowedFormats }) {
				Failures.Compatibility("Un fichier importé ne peut pas accepter des formats refusés par sa source : ${
					allowedFormats
						.asSequence()
						.filter { it !in source.allowedFormats }
						.flatMap { it.extensions }
						.joinToString(", ")
				}")
			}
		}

		companion object {
			const val maxAllowedSizeMB = 10
			val defaultExpiresAfter = 365.days
		}

		enum class Format(vararg formats: Pair<List<String>, List<String>>) {
			//  extensions   to   MIME types
			Image(
				listOf("JPEG", "JPG") to listOf("image/jpeg"),
				listOf("PNG") to listOf("image/png"),
				listOf("BMP") to listOf("image/bmp"),
				listOf("WEBP") to listOf("image/webp"),
			),
			Document(
				listOf("PDF") to listOf("application/pdf"),
			),
			Archive(
				listOf("ZIP") to listOf("application/zip", "application/x-zip-compressed"),
			),
			Audio(
				listOf("MP3") to listOf("audio/mpeg"),
				listOf("OGG") to listOf("audio/ogg"),
				listOf("WAV") to listOf("audio/wav"),
				listOf("WEBA") to listOf("audio/webm"),
			),
			Video(
				listOf("AVI") to listOf("video/x-msvideo"),
				listOf("MP4") to listOf("video/mp4"),
				listOf("OGV") to listOf("video/ogg"),
				listOf("WEBM") to listOf("video/webm"),
			),
			Tabular(
				listOf("CSV") to listOf("text/csv"),
			),
			Event(
				listOf("ICS") to listOf("text/calendar"),
			),
			;

			private val formats = formats.toMap()
			val extensions get() = formats.keys.asSequence().flatten()
			val mimeTypes get() = formats.values.asSequence().flatten()

			fun allowsMimeType(mime: String) = mime in mimeTypes
			fun allowsFilename(filename: String) = extensions.any { it.endsWith(".$filename", ignoreCase = true) }

			companion object {
				fun Iterable<Format>.allowsMimeType(mime: String) = any { it.allowsMimeType(mime) }
				fun Iterable<Format>.allowsFilename(filename: String) = any { it.allowsFilename(filename) }
			}
		}
	}

	sealed interface Failures {
		// For now, errors are just strings.
		// I will maybe replace them by proper sealed class hierarchies in the future if needed.

		data class Creating(val message: String) : Failures

		data class Parsing(val message: String, val cause: Any? = null) : Failures

		data class Compatibility(val message: String) : Failures
	}

	companion object
}

private inline fun <T> Raise<Input.Failures.Parsing>.captureParsingFailure(block: () -> T): T = try {
	block()
} catch (e: IllegalArgumentException) {
	raise(Input.Failures.Parsing(e.message ?: "Pas de message"))
}

private inline fun <T> captureCreationFailure(block: () -> T): Outcome<Input.Failures.Creating, T> = try {
	block().success()
} catch (e: IllegalArgumentException) {
	Input.Failures.Creating(e.message ?: "Pas de message").failed()
}

fun Input.Companion.text(maxLength: UInt? = null) = captureCreationFailure { Input.Text(maxLength) }
fun Input.Companion.integer(min: Long? = null, max: Long? = null) = captureCreationFailure { Input.Integer(min, max) }
fun Input.Companion.upload(allowedFormats: Set<Input.Upload.Format>, maxSizeMB: Int? = null, expiresAfter: Duration? = null) = captureCreationFailure { Input.Upload(allowedFormats, maxSizeMB, expiresAfter) }
