package opensavvy.formulaide.database

import kotlinx.coroutines.currentCoroutineContext

suspend fun testDatabase() = Database(currentCoroutineContext())
