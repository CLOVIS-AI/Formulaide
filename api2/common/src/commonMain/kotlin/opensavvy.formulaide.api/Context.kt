package opensavvy.formulaide.api

import opensavvy.formulaide.core.User

/**
 * The Formulaide request context (which user is connected…).
 */
class Context(
	val user: User.Ref?,
	val role: User.Role,
)
