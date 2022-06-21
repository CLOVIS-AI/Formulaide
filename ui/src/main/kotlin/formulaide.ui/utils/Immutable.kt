package formulaide.ui.utils

import formulaide.api.fields.Field
import formulaide.ui.fields.editors.SwitchDirection

fun <T> List<T>.replace(index: Int, value: T): List<T> =
	ArrayList<T>(size).also {
		it.addAll(subList(0, index))
		it.add(value)
		it.addAll(subList(index + 1, size))
	}

fun <T> List<T>.remove(index: Int): List<T> =
	ArrayList<T>(size).also {
		it.addAll(subList(0, index))
		it.addAll(subList(index + 1, size))
	}

fun <T : Field> List<T>.switchOrder(index: Int, direction: SwitchDirection): List<T> {
	val otherIndex = index + direction.offset
	val other = getOrNull(otherIndex)

	return if (other != null) {
		@Suppress("UNCHECKED_CAST") // requestCopy doesn't change the type
		replace(index, get(index).requestCopy(order = other.order) as T)
			.replace(otherIndex, other.requestCopy(order = get(index).order) as T)
			.sortedBy { it.order }
	} else this
}
