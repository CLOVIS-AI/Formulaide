package opensavvy.formulaide.mongo

import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Input
import kotlin.time.Duration


/**
 * DTO for [Input].
 */
@Serializable
class InputDbDto(
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

        fun InputDbDto.toCore() = when (type) {
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
            Input.Date -> InputDbDto(Type.Date)
            Input.Email -> InputDbDto(Type.Email)
            is Input.Integer -> InputDbDto(Type.Integer, minValue = min, maxValue = max)
            Input.Phone -> InputDbDto(Type.Phone)
            is Input.Text -> InputDbDto(Type.Text, maxLength = maxLength)
            Input.Time -> InputDbDto(Type.Time)
            Input.Toggle -> InputDbDto(Type.Toggle)
            is Input.Upload -> InputDbDto(
                Type.Upload,
                maxSizeMB = maxSizeMB,
                expiresAfter = expiresAfter,
                formats = allowedFormats.flatMap { it.mimeTypes })
        }

    }
}
