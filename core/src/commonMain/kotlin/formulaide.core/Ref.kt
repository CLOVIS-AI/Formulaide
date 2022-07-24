package formulaide.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import opensavvy.backbone.Backbone

/**
 * Single implementation of the [Backbone Ref][opensavvy.backbone.Ref].
 *
 * Backbone usually expects multiple Ref implementation (one for each service), but there is only a single Formulaide
 * implementation, so this is unnecessary.
 */
data class Ref<O>(
	val id: String,
	override val backbone: Backbone<O>,
) : opensavvy.backbone.Ref<O> {

	class Serializer<O>(private val backbone: Backbone<O>) : KSerializer<Ref<O>> {
		override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Ref", PrimitiveKind.STRING)

		override fun deserialize(decoder: Decoder): Ref<O> {
			val id = decoder.decodeString()
			return Ref(id, backbone)
		}

		override fun serialize(encoder: Encoder, value: Ref<O>) {
			encoder.encodeString(value.id)
		}
	}
}
