package opensavvy.formulaide.remote.dto

import kotlinx.serialization.Serializable
import opensavvy.formulaide.core.Input

/**
 * DTO for [Input].
 */
@Serializable
class InputDto(
	val type: Type,
	val maxLength: UInt? = null,
	val minValue: Long? = null,
	val maxValue: Long? = null,
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
		}

		fun Input.toDto() = when (this) {
			Input.Date -> InputDto(Type.Date)
			Input.Email -> InputDto(Type.Email)
			is Input.Integer -> InputDto(Type.Integer, minValue = min, maxValue = max)
			Input.Phone -> InputDto(Type.Phone)
			is Input.Text -> InputDto(Type.Text, maxLength = maxLength)
			Input.Time -> InputDto(Type.Time)
			Input.Toggle -> InputDto(Type.Toggle)
		}

	}
}
