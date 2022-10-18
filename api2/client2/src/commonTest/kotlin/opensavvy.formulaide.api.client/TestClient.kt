package opensavvy.formulaide.api.client

import kotlinx.coroutines.currentCoroutineContext

suspend fun testClient() = Client("http://localhost:8000", currentCoroutineContext())
