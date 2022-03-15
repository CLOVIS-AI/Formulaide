package formulaide.api.data

import formulaide.api.types.Email
import kotlinx.serialization.Serializable

/**
 * Represents different configuration options that need to be available in the UI but can only be configured on the server.
 */
@Serializable
data class Config(
	val reportEmail: Email?,
	val helpURL: String?,
	val pdfLeftImageURL: String?,
	val pdfRightImageURL: String?,
) {

	companion object {
		val defaultReportEmail = Email("contact-project+opensavvy-formulaide-33369420-issue-@incoming.gitlab.com")
		const val defaultHelpUrl = "https://opensavvy.gitlab.io/formulaide/docs/user-guide.pdf"
	}
}

val Config?.reportEmailOrDefault
	get() = this?.reportEmail ?: Config.defaultReportEmail

val Config?.helpUrlOrDefault
	get() = this?.helpURL ?: Config.defaultHelpUrl
