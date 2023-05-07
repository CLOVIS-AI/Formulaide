package opensavvy.formulaide.core.data

/**
 * Marker interface for failures caused by the lack of authentication when executing an operation which requires authentication.
 */
interface StandardUnauthenticated

/**
 * Marker interface for failures caused by insufficient rights requested by the current user.
 */
interface StandardUnauthorized

/**
 * Marker interface for failures caused by the request of a [id] that doesn't exist.
 */
interface StandardNotFound<T> {
	val id: T
}
