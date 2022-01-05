package formulaide.ui.components

import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.WriteDelegatedProperty
import formulaide.ui.utils.onSet
import kotlinx.browser.window
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set
import react.ChildrenBuilder
import react.useState

val localStorageSerializer = Json.Default

/**
 * [useState]-like hook that stores its value permanently in [LocalStorage][org.w3c.dom.WindowLocalStorage].
 *
 * Values are stored in JSON, serialized by [localStorageSerializer].
 *
 * @param id Unique ID of that value
 * @param defaultValue Original value when LocalStorage is empty
 * @see clearLocalStorage
 */
@Suppress("unused") // Type safety: only call in an FC
inline fun <reified T : Any> ChildrenBuilder.useLocalStorage(
	id: String,
	defaultValue: T,
): WriteDelegatedProperty<T> {
	val state = useState(
		window.localStorage[id]
			?.let { localStorageSerializer.decodeFromString(it) }
			?: defaultValue
	)

	return state.asDelegated()
		.onSet { window.localStorage[id] = localStorageSerializer.encodeToString(it) }
}

/**
 * Clears the LocalStorage used by [useLocalStorage].
 */
fun clearLocalStorage(id: String) {
	window.localStorage.removeItem(id)
}
