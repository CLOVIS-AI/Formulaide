package formulaide.ui.utils

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
