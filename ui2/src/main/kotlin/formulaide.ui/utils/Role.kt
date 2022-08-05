package formulaide.ui.utils

import formulaide.api.users.User
import formulaide.client.Client

enum class Role {
	ANONYMOUS,
	EMPLOYEE,
	ADMINISTRATOR,
	;

	companion object {
		val User?.role
			get() = when {
				this == null -> ANONYMOUS
				!administrator -> EMPLOYEE
				administrator -> ADMINISTRATOR
				else -> error("Should never happen")
			}

		val Client.role
			get() = when (this) {
				is Client.Anonymous -> ANONYMOUS
				is Client.Authenticated -> me.role
			}
	}
}
