package formulaide.client

actual suspend fun Client.refreshToken(): String? {
	System.err.println("WARNING. The JVM implementation of refreshToken is currently a noop")
	return null
}
