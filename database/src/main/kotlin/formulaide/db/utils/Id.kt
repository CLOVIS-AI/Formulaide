package formulaide.db.utils

import org.litote.kmongo.id.IdGenerator

fun <T> generateId(): String =
	IdGenerator.defaultGenerator.generateNewId<T>().toString()
