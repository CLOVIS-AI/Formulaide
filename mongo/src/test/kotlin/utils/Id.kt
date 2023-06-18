package opensavvy.formulaide.mongo.utils

import org.litote.kmongo.newId

val commonIds = Array(20) { newId<Unit>().toString() }
