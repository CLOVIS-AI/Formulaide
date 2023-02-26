package opensavvy.formulaide.remote.dto

import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Input
import kotlin.time.Duration

/**
 * DTO for [Input].
 */
@Serializable
class InputDto(
	val type: Type,
	val maxLength: UInt? = null,
	val minValue: Long? = null,
	val maxValue: Long? = null,
	val maxSizeMB: Int? = null,
	val expiresAfter: Duration? = null,
	val formats: List<String>? = null,
) {

	@Serializable
	enum class Type {
		Text,
		Integer,
		Toggle,
		Email,
		Phone,
		Date,
		Time,
		Upload,
	}

	companion object {

		fun InputDto.toCore() = when (type) {
			Type.Text -> Input.Text(maxLength)
			Type.Integer -> Input.Integer(minValue, maxValue)
			Type.Toggle -> Input.Toggle
			Type.Email -> Input.Email
			Type.Phone -> Input.Phone
			Type.Date -> Input.Date
			Type.Time -> Input.Time
			Type.Upload -> Input.Upload(
				Input.Upload.Format.values().asSequence()
					.filter { format -> format.mimeTypes.any { it in formats!! } }
					.toSet(),
				maxSizeMB,
				expiresAfter,
			)
		}

		fun Input.toDto() = when (this) {
			Input.Date -> InputDto(Type.Date)
			Input.Email -> InputDto(Type.Email)
			is Input.Integer -> InputDto(Type.Integer, minValue = min, maxValue = max)
			Input.Phone -> InputDto(Type.Phone)
			is Input.Text -> InputDto(Type.Text, maxLength = maxLength)
			Input.Time -> InputDto(Type.Time)
			Input.Toggle -> InputDto(Type.Toggle)
			is Input.Upload -> InputDto(
				Type.Upload,
				maxSizeMB = maxSizeMB,
				expiresAfter = expiresAfter,
				formats = allowedFormats.flatMap { it.mimeTypes })
		}

	}
}
