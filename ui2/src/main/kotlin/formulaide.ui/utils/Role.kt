package formulaide.ui.utils

import formulaide.api.users.User

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
	}
}
