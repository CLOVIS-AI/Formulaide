package formulaide.api

import formulaide.core.User

class Context(
	val role: User.Role,
	val email: String?,
)
