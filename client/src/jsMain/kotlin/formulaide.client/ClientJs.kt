package formulaide.client

import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import org.w3c.fetch.INCLUDE
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit

actual suspend fun Client.refreshToken(): String? {
	val response = window.fetch(
		"$hostUrl/api/auth/refreshToken",
		RequestInit(
			method = "POST",
			credentials = RequestCredentials.INCLUDE
		)
	).await()

	return when {
		response.ok -> Client.jsonSerializer
			.decodeFromString<String>(response.text().await())
		else -> {
			console.warn("La récupération de l'access token a échoué", response)
			null
		}
	}
}
