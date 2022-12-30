package opensavvy.formulaide.test.assertions

import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.request
import opensavvy.state.firstValue

suspend fun <T> Ref<T>.now() = request().firstValue()
