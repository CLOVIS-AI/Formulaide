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
	private val onUpdate: ((T) -> T) -> Unit,
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

fun <I : Any, O : Any> DelegatedProperty<I>.map(transform: (I) -> O) =
	ReadDelegatedProperty { transform(value) }

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
		onUpdate = { newValueGenerator ->
			update {
				newValueGenerator(this@update).also(listener)
			}
		}
	)

/**
 * Cancel the update if the new value is equal to the previous one.
 *
 * This allows to decrease the number of renders for list modifications.
 *
 * @see Any.equals
 * @see Any.hashCode
 */
fun <T> WriteDelegatedProperty<T>.useEquals() =
	WriteDelegatedProperty(
		reader = reader,
		onUpdate = { newValueGenerator ->
			val old = reader.value
			val new = newValueGenerator(old)

			// Swallow the update if the object is the same reference
			if (new === old)
				Unit

			// Test hashCode before testing equals
			else if (new.hashCode() == old.hashCode() && new == old)
				console.log("Cancelled state update because objects are the same according to 'equals'.")
			else
				update { new }
		}
	)

/**
 * When the list is replaced by another one, prefer keeping the element in the original list (if it was not edited), rather than replacing it by the new version.
 *
 * Because React always compares objects with referential equality, this can significantly reduce renders of child components when using the result of an API endpoint in `useState` or similar.
 *
 * @see useEquals
 * @see Any.equals
 * @see Any.hashCode
 */
fun <T> WriteDelegatedProperty<List<T>>.useListEquality() =
	WriteDelegatedProperty(
		reader = reader,
		onUpdate = { newValueGenerator ->
			val old = reader.value
			val new = newValueGenerator(old)

			val result = ArrayList<T>(new.size)
			var i = 0
			while (i < new.size && i < old.size) {
				val o = old[i]
				val n = new[i]

				result.add(
					if (o.hashCode() == n.hashCode() && o == n) o
					else n
				)

				i++
			}
			while (i < new.size) {
				result.add(new[i])

				i++
			}

			check(result == new) { "L'optimisation des listes ne devrait pas modifier les éléments de la liste." }
			update { result }
		}
	)

//endregion
