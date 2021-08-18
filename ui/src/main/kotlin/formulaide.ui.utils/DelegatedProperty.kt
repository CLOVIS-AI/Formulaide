package formulaide.ui.utils

import kotlin.reflect.KProperty

interface DelegatedProperty<out T> {
	val value: T
	operator fun component1(): T
	operator fun getValue(thisRef: Nothing?, property: KProperty<*>): T

	@Suppress("FunctionName") // Factories that look like constructors
	companion object {
		fun <T> DelegatedProperty(get: () -> T) = ReadDelegatedProperty(get)
		fun <T> DelegatedProperty(get: () -> T, set: (T) -> Unit) = WriteDelegatedProperty(get, set)
	}
}

class ReadDelegatedProperty<out T>(val get: () -> T) : DelegatedProperty<T> {

	// Normal access
	override val value get() = get()

	// Destructuration
	override operator fun component1() = value

	// Delegation
	override operator fun getValue(
		thisRef: Nothing?,
		property: KProperty<*>,
	) = get()

}

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
class WriteDelegatedProperty<T>(
	private val reader: ReadDelegatedProperty<T>,
	val set: (T) -> Unit,
) : DelegatedProperty<T> by reader {

	constructor(get: () -> T, set: (T) -> Unit) : this(ReadDelegatedProperty(get), set)

	// Normal access
	override var value: T
		get() = reader.value
		set(value) {
			set(value)
		}

	// Destructuration
	operator fun component2() = set

	// Delegation
	operator fun setValue(
		thisRef: Nothing?,
		property: KProperty<*>,
		value: T,
	) = set(value)

}

//region Extensions

fun <I : Any, O : Any?> DelegatedProperty<I?>.map(transform: (I) -> O) =
	ReadDelegatedProperty { value?.let(transform) }

fun <I> DelegatedProperty<I>.filter(predicate: (I) -> Boolean) =
	ReadDelegatedProperty { value.takeIf(predicate) }

inline fun <reified I> DelegatedProperty<*>.filterIs() =
	map { it as? I }

//endregion
