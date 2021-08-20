package formulaide.ui.utils

import react.StateInstance
import kotlin.reflect.KProperty

interface DelegatedProperty<out T> {
	val value: T
	operator fun component1(): T
	operator fun getValue(thisRef: Nothing?, property: KProperty<*>): T

	@Suppress("FunctionName") // Factories that look like constructors
	companion object {
		fun <T> DelegatedProperty(get: () -> T) =
			ReadDelegatedProperty(get)

		fun <T> DelegatedProperty(get: () -> T, onUpdate: ((T) -> T) -> Unit) =
			WriteDelegatedProperty(ReadDelegatedProperty(get), onUpdate)

		fun <T> StateInstance<T>.asDelegated() =
			WriteDelegatedProperty(ReadDelegatedProperty { component1() },
			                       onUpdate = { v -> component2().invoke(v) })
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
	val reader: ReadDelegatedProperty<T>,
	val onUpdate: ((T) -> T) -> Unit,
) : DelegatedProperty<T> by reader {

	// Normal access
	override var value: T
		get() = reader.value
		set(value) {
			onUpdate { value }
		}

	// Access that provides the current value, so it isn't captured in the caller's closure
	fun update(transform: T.() -> T) = onUpdate(transform)

	// Destructuration
	operator fun component2(): (T.() -> T) -> Unit = onUpdate

	// Delegation
	operator fun setValue(
		thisRef: Nothing?,
		property: KProperty<*>,
		value: T,
	) = onUpdate { value }

}

//region Extensions

fun <I : Any, O : Any?> DelegatedProperty<I?>.map(transform: (I) -> O) =
	ReadDelegatedProperty { value?.let(transform) }

fun <I> DelegatedProperty<I>.filter(predicate: (I) -> Boolean) =
	ReadDelegatedProperty { value.takeIf(predicate) }

inline fun <reified I> DelegatedProperty<*>.filterIs() =
	map { it as? I }

/**
 * Get notified when the [DelegatedProperty]'s setter is called.
 *
 * @param listener A callback executed with the new value.
 */
fun <T> WriteDelegatedProperty<T>.onSet(listener: (T) -> Unit) =
	WriteDelegatedProperty(
		reader = reader,
		onUpdate = { update ->
			onUpdate {
				update(it).also(listener)
			}
		}
	)

//endregion
