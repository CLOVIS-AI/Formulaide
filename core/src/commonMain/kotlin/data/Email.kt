package opensavvy.formulaide.core.data

data class Email(
	val value: String,
) {

	init {
		require('@' in value) { "Une adresse email doit contenir un arobase : '$value'" }
	}

	companion object {
		fun String.asEmail() = Email(this)
	}
}
