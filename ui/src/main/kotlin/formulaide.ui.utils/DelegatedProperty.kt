package formulaide.ui.utils

import kotlin.reflect.KProperty

/**
 * Enables Kotlin property syntax sugar for a custom property with no backing field.
 *
 * ## Examples
 *
 * In all examples, we assume the existence of a valid [DelegatedProperty]:
 * ```kotlin
 * val p = DelegatedProperty<Int>( … )
 * ```
 *
 * ### Example 1: normal access
 * ```kotlin
 * println(p.get()) // read via the getter
 * println(p.value) // read as a property
 * p.set(5)         // update
 * ```
 *
 * ### Example 2: destructuration
 * ```kotlin
 * val (v, setV) = p
 * println(v)       // read
 * setV(5)          // write
 * ```
 *
 * ### Example 3: delegation
 * ```kotlin
 * val v by p
 * println(v)       // read
 * v = 5            // write
 * ```
 */
class DelegatedProperty<T>(val get: () -> T, val set: (T) -> Unit) {

	// Normal access
	val value get() = get()

	// Destructuration
	operator fun component1() = value
	operator fun component2() = set

	// Delegation
	operator fun getValue(
		thisRef: Nothing?,
		property: KProperty<*>,
	) = get()

	operator fun setValue(
		thisRef: Nothing?,
		property: KProperty<*>,
		value: T,
	) = set(value)

}
