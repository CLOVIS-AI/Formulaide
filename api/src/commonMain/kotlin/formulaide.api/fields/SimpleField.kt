package formulaide.api.fields

import formulaide.api.data.FormSubmission
import formulaide.api.fields.SimpleField.Message.arity
import formulaide.api.fields.SimpleField.Upload.Format
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import formulaide.api.types.Date as ApiDate
import formulaide.api.types.Email as ApiEmail
import formulaide.api.types.Time as ApiTime
import formulaide.api.types.Upload as ApiUpload

/**
 * A field that represents some specific data.
 *
 * This class does not implement [Field], because it is meant to be wrapped inside a [Field] implementation:
 * see [DataField.Simple], [ShallowFormField.Simple] and [DeepFormField.Simple].
 * For this reason, this class doesn't have an [id][Field.id] nor a [name][Field.name].
 */
@Serializable
sealed class SimpleField {
	abstract val arity: Arity

	/**
	 * Checks that a [value] provided by the user is compatible with this type.
	 */
	abstract fun parse(value: String?): Any?

	/**
	 * Checks that [newer] is compatible with the current [SimpleField].
	 *
	 * Use this to check that a [FormField] corresponds to the matching [DataField], for example.
	 */
	open fun validateCompatibility(newer: SimpleField) {
		require(this::class == newer::class) { "Une donnée ne peut pas être compatible avec une donnée d'un autre type : la valeur d'origine est de type ${this::class.simpleName}, la nouvelle est de type ${newer::class.simpleName}" }

		require(newer.arity.min >= arity.min) { "Une donnée ne peut qu'augmenter l'arité minimale : la valeur d'origine ($arity) autorise un espace moins large que la nouvelle valeur (${newer.arity})" }
		require(newer.arity.max <= arity.max) { "Une donnée ne peut que diminuer l'arité maximale : la valeur d'origine ($arity) autorise un espace moins large que la nouvelle valeur (${newer.arity})" }
	}

	abstract fun requestCopy(arity: Arity? = null, defaultValue: String? = null): SimpleField

	/**
	 * If this field wasn't filled in by the user, and [defaultValue] isn't `null`, then it should be displayed during review.
	 *
	 * This can be used to create [mandatory][Arity] fields when editing a form, as [FormSubmission.parse] will use this value instead of refusing non-filled fields.
	 */
	abstract val defaultValue: String?

	fun validate() {
		if (defaultValue != null)
			parse(defaultValue)
	}

	/**
	 * The user should input some text.
	 * @property maxLength The maximum number of characters allowed (`null` means 'no limit').
	 */
	@Serializable
	@SerialName("TEXT")
	data class Text(
		override val arity: Arity,
		val maxLength: Int? = null,
		override val defaultValue: String? = null,
	) : SimpleField() {

		val effectiveMaxLength get() = maxLength ?: Int.MAX_VALUE

		init {
			validate()
		}

		override fun parse(value: String?): String {
			requireNotNull(value) { "Un champ de texte doit être rempli : trouvé '$value'" }
			require(value.isNotBlank()) { "Un champ de texte ne peut pas être vide ou contenir uniquement des espaces : trouvé '$value'" }

			require(value.length <= effectiveMaxLength) { "La longueur maximale autorisée est de $maxLength caractères, mais ${value.length} ont été donnés" }

			return value
		}

		override fun validateCompatibility(newer: SimpleField) {
			super.validateCompatibility(newer)
			newer as Text

			require(newer.effectiveMaxLength <= effectiveMaxLength) { "La longueur maximale ne peut être que diminuée : la valeur d'origine est $effectiveMaxLength, la nouvelle valeur est ${newer.effectiveMaxLength}" }
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)
	}

	/**
	 * The user should input an integer (Kotlin's [Long]).
	 * @property min The minimum value of that integer (`null` means 'no minimum').
	 * @property max The maximum value of that integer (`null` means 'no maximum').
	 */
	@Serializable
	@SerialName("INTEGER")
	data class Integer(
		override val arity: Arity,
		val min: Long? = null,
		val max: Long? = null,
		override val defaultValue: String? = null,
	) : SimpleField() {

		val effectiveMin get() = min ?: Long.MIN_VALUE
		val effectiveMax get() = max ?: Long.MAX_VALUE

		init {
			validate()
		}

		override fun parse(value: String?): Long {
			requireNotNull(value) { "Un entier ne peut pas être vide : trouvé $value" }
			val intVal =
				requireNotNull(value.toLongOrNull()) { "Cette donnée n'est pas un entier : $value" }

			require(intVal >= effectiveMin) { "La valeur minimale autorisée est $min, $intVal est trop petit" }
			require(intVal <= effectiveMax) { "La valeur maximale autorisée est $max, $intVal est trop grand" }

			return intVal
		}

		override fun validateCompatibility(newer: SimpleField) {
			super.validateCompatibility(newer)
			newer as Integer

			require(effectiveMin <= newer.effectiveMin) { "La valeur minimale ne peut pas être diminuée : la valeur d'origine est $effectiveMin, la nouvelle valeur est ${newer.effectiveMin}" }
			require(effectiveMax >= newer.effectiveMax) { "La valeur maximale ne peut pas être augmentée : la valeur d'origine est $effectiveMax, la nouvelle valeur est ${newer.effectiveMax}" }
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)
	}

	/**
	 * The user should input a decimal number (Kotlin's [Double]).
	 */
	@Serializable
	@SerialName("DECIMAL")
	data class Decimal(
		override val arity: Arity,
		override val defaultValue: String? = null,
	) : SimpleField() {

		init {
			validate()
		}

		override fun parse(value: String?): Double {
			requireNotNull(value) { "Un réel ne peut pas être vide : trouvé $value" }

			return requireNotNull(value.toDoubleOrNull()) { "Cette donnée n'est pas un réel : $value" }
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)
	}

	/**
	 * The user should check a box.
	 */
	@Serializable
	@SerialName("BOOLEAN")
	data class Boolean(
		override val arity: Arity,
		override val defaultValue: String? = null,
	) : SimpleField() {

		init {
			validate()
		}

		override fun parse(value: String?): kotlin.Boolean {
			requireNotNull(value) { "Un booléen ne peut pas être vide : trouvé $value" }
			return requireNotNull(value.toBooleanStrictOrNull()) { "Cette donnée n'est pas un booléen : $value" }
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)
	}

	@Serializable
	@SerialName("EMAIL")
	data class Email(
		override val arity: Arity,
		override val defaultValue: String? = null,
	) : SimpleField() {

		init {
			validate()
		}

		override fun parse(value: String?): ApiEmail {
			requireNotNull(value) { "Un email ne peut pas être vide : trouvé $value" }
			return ApiEmail(value)
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)
	}

	@Serializable
	@SerialName("TELEPHONE")
	data class Phone(
		override val arity: Arity,
		override val defaultValue: String? = null,
	) : SimpleField() {

		init {
			validate()
		}

		override fun parse(value: String?): String {
			requireNotNull(value) { "Un numéro de téléphone ne peut pas être vide : trouvé $value" }
			require(value.isNotBlank()) { "Un numéro de téléphone ne peut pas être vide : trouvé '$value'" }
			return value
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)
	}

	@Serializable
	@SerialName("DATE")
	data class Date(
		override val arity: Arity,
		override val defaultValue: String? = null,
	) : SimpleField() {

		init {
			validate()
		}

		override fun parse(value: String?): ApiDate {
			requireNotNull(value) { "Une date ne peut pas être vide : trouvé $value" }

			val parts = value.split('-')
			require(parts.size == 3) { "Une date doit être composée de 3 parties, séparées par des tirets : trouvé $value" }

			val (year, month, day) = parts.map { it.toIntOrNull() }
			requireNotNull(year) { "L'année devrait être un entier : $year" }
			requireNotNull(month) { "Le mois devrait être un entier : $month" }
			requireNotNull(day) { "Le jour devrait être un entier : $day" }

			return ApiDate(year, month, day)
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)
	}

	@Serializable
	@SerialName("TIME")
	data class Time(
		override val arity: Arity,
		override val defaultValue: String? = null,
	) : SimpleField() {

		init {
			validate()
		}

		override fun parse(value: String?): ApiTime {
			requireNotNull(value) { "Une heure ne peut pas être vide : trouvé $value" }

			val parts = value.split(':')
			require(parts.size == 2) { "Une heure doit être composée de deux parties, séparées par des ':' : trouvé $value" }

			val (hour, minutes) = parts.map { it.toIntOrNull() }
			requireNotNull(hour) { "L'heure devrait être un entier : $hour" }
			requireNotNull(minutes) { "Les minutes devraient être un entier : $minutes" }

			return ApiTime(hour, minutes)
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)
	}

	/**
	 * A message should be displayed to the user, but they shouldn't have anything to fill in.
	 * The [arity] is always [Arity.mandatory].
	 */
	@Serializable
	@SerialName("MESSAGE")
	object Message : SimpleField() {
		override val arity get() = Arity.mandatory()
		override fun parse(value: String?): Nothing? = null // whatever is always valid

		override val defaultValue: String? = null

		override fun toString() = "Message"
		override fun requestCopy(arity: Arity?, defaultValue: String?) =
			this // there's no arity or default value here
	}

	/**
	 * A file uploaded by the user.
	 *
	 * @property allowedFormats List of allowed [formats][Format]
	 * @property maxSizeMB The maximal size of files, in MB (1..10)
	 * @property expiresAfterDays The time before the file is removed (to comply with RGPD, in days)
	 * @see ApiUpload
	 */
	@Serializable
	@SerialName("FILE_UPLOAD")
	data class Upload(
		override val arity: Arity,
		val allowedFormats: List<Format>,
		val maxSizeMB: Int? = null,
		val expiresAfterDays: Int? = null,
		override val defaultValue: String? = null,
	) : SimpleField() {

		init {
			require(effectiveMaxSizeMB in 1..10) { "Une pièce jointe doit avoir une taille comprise entre 1 et 10 Mo : trouvé $effectiveMaxSizeMB Mo" }
			require(effectiveExpiresAfterDays in 1..5000) { "Une pièce jointe ne peut pas avoir une date d'expiration de tant de temps : trouvé $effectiveExpiresAfterDays jours" }

			validate()
		}

		val effectiveMaxSizeMB get() = maxSizeMB ?: 10
		val effectiveExpiresAfterDays get() = expiresAfterDays ?: (30 * 12)

		override fun parse(value: String?): Ref<ApiUpload> {
			requireNotNull(value) { "Un fichier doit avoir des coordonnées : trouvé $value" }

			return Ref(value)
		}

		override fun requestCopy(arity: Arity?, defaultValue: String?) = copy(
			arity = arity ?: this.arity,
			defaultValue = defaultValue ?: this.defaultValue,
		)

		// These formats were selected because they are often used, and the employees likely already have everything needed to open them.
		@Serializable
		enum class Format(vararg formats: Pair<List<String>, String>) {
			IMAGE(
				listOf("JPEG", "JPG") to "image/jpeg",
				listOf("PNG") to "image/png",
				listOf("BMP") to "image/bmp",
				listOf("TIF", "TIFF") to "image/tiff",
				listOf("WEBP") to "image/webp",
			),
			DOCUMENT(
				listOf("PDF") to "application/pdf",
				listOf("TXT") to "text/plain",
				listOf("RTF") to "application/rtf",
			),
			ARCHIVE(
				listOf("ZIP") to "application/zip",
			),
			AUDIO(
				listOf("MP3") to "audio/mpeg",
				listOf("OGG") to "audio/ogg",
				listOf("WAV") to "audio/wav",
				listOf("WEBA") to "audio/webm",
			),
			VIDEO(
				listOf("AVI") to "video/x-msvideo",
				listOf("MP4") to "video/mp4",
				listOf("OGV") to "video/ogg",
				listOf("WEBM") to "video/webm",
			),
			TABULAR(
				listOf("CSV") to "text/csv",
			),
			EVENT(
				listOf("ICS") to "text/calendar",
			),
			;

			val formats = formats.toMap()
			val extensions get() = formats.keys.flatten()
			val mimeTypes get() = formats.values

			companion object {
				/**
				 * Checks whether the given [mime] type corresponds to one of the provided [formats][Format].
				 */
				fun List<Format>.allowsContentType(mime: String) =
					any { mime in it.mimeTypes }

				/**
				 * Checks whether the given [file]'s extension is allowed by one of the provided [formats][Format].
				 */
				fun List<Format>.allowsFilename(file: String) =
					any { format -> format.extensions.any { file.endsWith(it, ignoreCase = true) } }
			}
		}
	}

}
