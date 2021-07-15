package formulaide.client

import formulaide.api.users.TokenResponse
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.decodeFromString
import org.w3c.fetch.INCLUDE
import org.w3c.fetch.RequestCredentials
import org.w3c.fetch.RequestInit

actual suspend fun Client.refreshToken(): String? {
	val response = window.fetch(
		"$hostUrl/users/refreshToken",
		RequestInit(
			method = "POST",
			credentials = RequestCredentials.INCLUDE
		)
	).await()

	return when {
		response.ok -> Client.jsonSerializer
			.decodeFromString<TokenResponse>(response.text().await())
			.token
		else -> {
			console.warn("La récupération du refresh token a échoué", response)
			null
		}
	}
}
