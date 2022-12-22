package opensavvy.formulaide.fake.utils

import kotlin.random.Random
import kotlin.random.nextUInt

internal fun newId(): String = Random.nextUInt().toString(16)
