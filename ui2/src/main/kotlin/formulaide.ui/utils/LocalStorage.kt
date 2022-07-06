package formulaide.ui.utils

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import formulaide.ui.utils.LocalStorageState.Companion.localStorageOf
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

/**
 * Stores a value permanently in the browser's Local Storage.
 *
 * Instantiate an instance of this class via the [provided factory][localStorageOf].
 */
class LocalStorageState<T>(private val key: String, value: T, private val listener: (T) -> String) :
	MutableState<T> {
	private var state by mutableStateOf(value)

	override var value: T
		get() = state
		set(value) {
			window.localStorage[key] = listener(value)
			state = value
		}

	override fun component1(): T = value
	override fun component2(): (T) -> Unit = { value = it }

	companion object {
		/**
		 * Creates a mutable state backed in the local storage.
		 */
		inline fun <reified T> localStorageOf(key: String, initialValue: T): LocalStorageState<T> {
			val stored = window.localStorage[key]

			val value = if (stored != null)
				try {
					Json.decodeFromString(stored)
				} catch (e: Exception) {
					console.warn("Ignoring invalid value from local storage for key '$key': '$stored'", e)
					initialValue
				}
			else
				initialValue

			return LocalStorageState(key, value, listener = { Json.encodeToString(it) })
		}
	}
}
