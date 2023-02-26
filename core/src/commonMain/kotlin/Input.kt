package opensavvy.formulaide.core

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import opensavvy.backbone.Ref.Companion.now
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import opensavvy.formulaide.core.data.Email as DataEmail

/**
 * A simple piece of information requested of the user filling in the form.
 */
sealed class Input {

	abstract suspend fun parse(value: String, uploads: File.Service): Outcome<Any>

	/**
	 * An input is compatible with its [source] if it is more restrictive (or equivalent).
	 */
	abstract suspend fun validateCompatibleWith(source: Input): Outcome<Unit>

	data class Text(
		val maxLength: UInt? = null,
	) : Input() {
		val effectiveMaxLength get() = maxLength ?: 4096u

		override suspend fun parse(value: String, uploads: File.Service) = out {
			ensureValid(value.length <= effectiveMaxLength.toInt()) { "Le texte saisi fait plus de $effectiveMaxLength caractères : ${value.length}" }
			value
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Text) { "Impossible d'importer un texte à partir d'un ${source::class} (${this@Text} -> $source)" }

			ensureValid(effectiveMaxLength <= source.effectiveMaxLength) { "Un texte importé ne peut pas autoriser une longueur ($effectiveMaxLength) plus élevée que celle de sa source (${source.effectiveMaxLength})" }
		}

		override fun toString() = "Texte (longueur maximale : $effectiveMaxLength)"
	}

	data class Integer(
		val min: Long? = null,
		val max: Long? = null,
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
			ensureValid(long != null) { "'$value' n'est pas un nombre valide" }

			ensureValid(long >= effectiveMin) { "$value est inférieur à la valeur minimale autorisée, $effectiveMin" }
			ensureValid(long <= effectiveMax) { "$value est supérieur à la valeur maximale autorisée, $effectiveMax" }

			long
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Integer) { "Impossible d'importer un texte à partir d'un ${source::class} (${this@Integer} -> $source)" }

			ensureValid(effectiveMin >= source.effectiveMin) { "Un nombre importé ne peut pas autoriser une valeur minimale ($effectiveMin) plus petite que celle de sa source (${source.effectiveMin})" }
			ensureValid(effectiveMax <= source.effectiveMax) { "Un nombre importé ne peut pas autoriser une valeur maximale ($effectiveMax) plus grande que celle de sa source (${source.effectiveMax})" }
		}

		override fun toString() = "Nombre (de $effectiveMin à $effectiveMax)"
	}

	object Toggle : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			val bool = value.toBooleanStrictOrNull()
			ensureValid(bool != null) { "'$value' n'est pas un booléen" }

			bool
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Toggle) { "Impossible d'importer un booléen à partir d'un ${source::class} (${this@Toggle} -> $source)" }
		}

		override fun toString() = "Coche"
	}

	object Email : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			DataEmail(value)
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Email) { "Impossible d'importer une adresse email à partir d'un ${source::class} (${this@Email} -> $source)" }
		}

		override fun toString() = "Adresse électronique"
	}

	object Phone : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			ensureValid(value.length <= 20) { "Un numéro de téléphone ne peut pas comporter ${value.length} caractères" }

			for (char in value) {
				ensureValid(char.isDigit() || char == '+') { "Le caractère '$char' n'est pas autorisé dans un numéro de téléphone" }
			}

			value
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Phone) { "Impossible d'importer un numéro de téléphone à partir d'un ${source::class} (${this@Phone} -> $source)" }
		}

		override fun toString() = "Numéro de téléphone"
	}

	object Date : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			LocalDate.parse(value)
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Date) { "Impossible d'importer une date à partir d'un ${source::class} (${this@Date} -> $source)" }
		}

		override fun toString() = "Date"
	}

	object Time : Input() {
		override suspend fun parse(value: String, uploads: File.Service) = out {
			LocalTime.parse(value)
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Time) { "Impossible d'importer une heure à partir d'un ${source::class} (${this@Time} -> $source)" }
		}

		override fun toString() = "Heure"
	}

	data class Upload(
		val allowedFormats: Set<Format>,
		val maxSizeMB: Int? = null,
		val expiresAfter: Duration? = null,
	) : Input() {

		val effectiveMaxSizeMB get() = maxSizeMB ?: maxAllowedSizeMB
		val effectiveExpiresAfter get() = expiresAfter ?: defaultExpiresAfter

		init {
			require(effectiveMaxSizeMB > 0) { "Il n'est pas possible d'exiger des pièces jointes de taille 0" }
			require(effectiveMaxSizeMB <= maxAllowedSizeMB) { "Une pièce jointe ne peut pas faire plus de $maxAllowedSizeMB Mo" }

			require(effectiveExpiresAfter < (31 * 12 * 10).days) { "Une pièce jointe ne peux pas être stockée plus de 10 ans" }
		}

		override suspend fun parse(value: String, uploads: File.Service): Outcome<File.Ref> = out {
			ensureValid(value.isNotBlank()) { "Un identifiant de fichier ne peut pas être vide : '$value'" }

			File.Ref(value, uploads)
				.also { it.now().bind() } // check that it exists
		}

		override suspend fun validateCompatibleWith(source: Input): Outcome<Unit> = out {
			ensureValid(source is Upload) { "Impossible d'importer une heure à partir d'un ${source::class} (${this@Upload} -> $source)" }

			ensureValid(effectiveMaxSizeMB <= source.effectiveMaxSizeMB) { "Un fichier importé ne peut pas autoriser une taille maximale ($effectiveMaxSizeMB Mo) plus grande que celle de sa source (${source.effectiveMaxSizeMB})" }
			ensureValid(effectiveExpiresAfter <= source.effectiveExpiresAfter) { "Un fichier importé ne peut pas être conservé (${effectiveExpiresAfter.inWholeDays} jours) plus grande que celle de sa source (${source.effectiveExpiresAfter} jours)" }
			ensureValid(allowedFormats.all { it in source.allowedFormats }) {
				"Un fichier importé ne peut pas accepter des formats refusés par sa source : ${
					allowedFormats
						.asSequence()
						.filter { it !in source.allowedFormats }
						.flatMap { it.extensions }
						.joinToString(", ")
				}"
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

}
