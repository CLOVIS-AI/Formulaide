package formulaide.api.types

/**
 * Expands the given [Arity] so its [min] is at most [newMin].
 *
 * If the [min] was already lesser than [newMin], no change is applied.
 */
fun Arity.expandMin(newMin: Int): Arity =
	if (min <= newMin) this
	else Arity(newMin, max)

/**
 * Expands the given [Arity] so its [max] is at least [newMax].
 *
 * If the [max] was already greater than [newMax], no change is applied.
 */
fun Arity.expandMax(newMax: Int): Arity =
	if (max >= newMax) this
	else Arity(min, newMax)

/**
 * Truncates the given [Arity] so its [min] is at least [newMin].
 *
 * If the [min] was already greater than [newMin], no change is applied.
 */
fun Arity.truncateMin(newMin: Int): Arity =
	if (min >= newMin) this
	else Arity(newMin, max)

/**
 * Truncates the given [Arity] so its [max] is at most [newMax].
 *
 * If the [max] was already less than [newMax], no change is applied.
 */
fun Arity.truncateMax(newMax: Int): Arity =
	if (max <= newMax) this
	else Arity(min, newMax)
