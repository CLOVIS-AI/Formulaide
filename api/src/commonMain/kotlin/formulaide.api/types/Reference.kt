package formulaide.api.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

typealias ReferenceId = String

/**
 * Some data that can be referenced from somewhere else.
 * @see id
 */
interface Referencable {

	/**
	 * A unique identifier for this object.
	 *
	 * The identifier only needs to be unique in the context of this object, not globally:
	 * for example, an object that can only appear in a specific list only has to have a unique identifier
	 * in that list.
	 */
	val id: ReferenceId
}

/**
 * A reference to something else.
 *
 * Only the [id] is serialized.
 * After deserialization, one can use [obj] to use the reference as a normal object instead.
 */
@Serializable
data class Ref<R : Referencable>(
	val id: ReferenceId,
) {

	/**
	 * Creates a reference and loads it automatically.
	 * @see createRef
	 */
	constructor(referenced: R) : this(referenced.id) {
		obj = referenced
	}

	@Transient // We don't want the object to be serialized, just its ID
	private var _obj: R? = null

	/**
	 * A convenience utility to use the object corresponding to this reference instead of the [id] itself.
	 *
	 * It is the developer's responsibility to provide the object corresponding to this reference (through [load], [loadFrom] or [obj]'s setter).
	 *
	 * It is allowed to load this reference multiple times (for example if the corresponding object is immutable and is replaced by a copy with the same [id]); any [load] operation overwrites the previous ones.
	 *
	 * One can use [loaded] to check whether this reference has been loaded or not (and therefore know if it is safe to access [obj]).
	 *
	 * @throws IllegalStateException When [obj] is queried before being initialized.
	 */
	var obj: R
		get() = _obj
			?: error("This reference ('$id') has not been loaded. You should load a reference before accessing it.")
		set(value) {
			require(value.id == id) { "The object given to this reference must have the same id as this reference; expected '$id' but found '${value.id}'" }
			_obj = value
		}

	/**
	 * `true` if `obj` is safe to access; `false` if accessing `obj` will throw an exception.
	 */
	val loaded: Boolean
		get() = _obj != null

	/**
	 * Loads an [object][obj] into this reference.
	 * Semantically equivalent to calling [obj]'s setter.
	 */
	fun load(obj: R) {
		this.obj = obj
	}

	/**
	 * Loads an object by finding it in [objs].
	 *
	 * If [allowNotFound] is set to `false`, the function will throw if [objs] doesn't have the correct object.
	 *
	 * When [lazy] is set to `true`, the function will only attempt to find one of the [objs] if the referenced is unloaded.
	 *
	 * @see loadIfNecessary
	 */
	fun loadFrom(objs: Iterable<R>, allowNotFound: Boolean = false, lazy: Boolean = false) {
		if (lazy && loaded)
			return

		val candidate = objs.find { it.id == id }

		if (candidate != null)
			obj = candidate
		else if (!allowNotFound)
			error("Couldn't find any object with id '$id' in the given list.")
	}

	override fun toString() = "Ref(id: $id, obj: ${if (loaded) "loaded" else "not loaded"})"

	companion object {

		/**
		 * Loads this reference by executing [loader], only if it has not been loaded yet.
		 */
		// inline to allow suspend lambdas
		inline fun <R : Referencable> Ref<R>.loadIfNecessary(
			lazy: Boolean = true,
			crossinline loader: (ReferenceId) -> R,
		) {
			if (!lazy || !loaded)
				load(loader(id))
		}

		/**
		 * Loads this reference by finding it in [objs], only if it has not been loaded yet.
		 */
		fun <R : Referencable> Ref<R>.loadIfNecessary(
			objs: Iterable<R>,
			allowNotFound: Boolean = false,
		) = loadFrom(objs, allowNotFound, lazy = true)

		/**
		 * Convenience method to get a reference on an object.
		 */
		fun <R : Referencable> R.createRef() = Ref(this)

		fun Iterable<Referencable>.ids() = map { it.id }

		/**
		 * Special reference that can be used for objects that are being created and that do not have an ID yet.
		 */
		const val SPECIAL_TOKEN_NEW: ReferenceId = "special:uninitialized"
	}
}
