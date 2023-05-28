package opensavvy.formulaide.remote.server.utils

import opensavvy.formulaide.core.utils.IdentifierWriter
import opensavvy.spine.SpineFailure

internal fun unauthenticated() = SpineFailure(SpineFailure.Type.Unauthenticated, "")

internal fun unauthorized() = SpineFailure(SpineFailure.Type.Unauthorized, "")

internal fun notFound(ref: IdentifierWriter) = SpineFailure(SpineFailure.Type.NotFound, ref.toIdentifier().text)

internal fun invalidRequest(text: String) = SpineFailure(SpineFailure.Type.InvalidRequest, text)

internal fun <T : Any> invalidRequest(payload: T?) = SpineFailure(SpineFailure.Type.InvalidRequest, payload)

internal fun invalidState(text: String) = SpineFailure(SpineFailure.Type.InvalidState, text)

internal fun <T : Any> invalidState(payload: T?) = SpineFailure(SpineFailure.Type.InvalidState, payload)
