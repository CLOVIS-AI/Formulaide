package opensavvy.formulaide.remote.failures

import kotlinx.serialization.Serializable
import opensavvy.spine.SpineFailure

@Serializable
class EmbeddedFailure<Payload : Any>(
	val type: SpineFailure.Type,
	val payload: Payload,
)

fun <Payload : Any> EmbeddedFailure<Payload>.asSpine() = SpineFailure(type, payload)

fun <Payload : Any> SpineFailure<Payload>.asEmbedded() = EmbeddedFailure(type, payload!!)

val <Payload : Any> SpineFailure<Payload>.message get() = (this as? SpineFailure.Message)?.message
