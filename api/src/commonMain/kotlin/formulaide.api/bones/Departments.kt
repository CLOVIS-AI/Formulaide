package formulaide.api.bones

import formulaide.api.users.Service
import formulaide.core.Department

fun Department.toLegacy() = Service(
	id,
	name,
	open,
)
