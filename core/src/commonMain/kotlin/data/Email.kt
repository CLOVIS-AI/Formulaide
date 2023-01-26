package opensavvy.formulaide.core.data

import kotlin.jvm.JvmInline

@JvmInline
value class Email(
	val value: String,
) {

	init {
		require('@' in value) { "Une adresse email doit contenir un arobase : '$value'" }
	}

	override fun toString() = value

	companion object {
		fun String.asEmail() = Email(this)
	}
}
