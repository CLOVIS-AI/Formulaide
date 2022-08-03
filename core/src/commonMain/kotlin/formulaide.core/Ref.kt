package formulaide.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import opensavvy.backbone.Ref

class RefSerializer<O, R : Ref<O>>(
	refName: String,
	private val fromId: (String) -> R,
	private val toId: (R) -> String,
) : KSerializer<R> {
	override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(refName, PrimitiveKind.STRING)

	override fun deserialize(decoder: Decoder): R {
		val id = decoder.decodeString()
		return fromId(id)
	}

	override fun serialize(encoder: Encoder, value: R) {
		encoder.encodeString(toId(value))
	}
}
