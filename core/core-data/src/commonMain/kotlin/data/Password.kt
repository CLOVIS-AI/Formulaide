package opensavvy.formulaide.core.data

import kotlin.jvm.JvmInline

@JvmInline
value class Password(val value: String) {

	init {
		require(value.length >= MIN_PASSWORD_SIZE) { "Un mot de passe doit faire au moins $MIN_PASSWORD_SIZE caractères : ${value.length} caractères trouvés" }
	}

	override fun toString() = "Password(REDACTED)"

	companion object {
		const val MIN_PASSWORD_SIZE = 8
	}
}
