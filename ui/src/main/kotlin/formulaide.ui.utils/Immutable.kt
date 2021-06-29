package formulaide.ui.utils

fun <T> List<T>.replace(index: Int, value: T): List<T> =
	ArrayList<T>(size).apply {
		addAll(this@replace.subList(0, index))
		add(value)
		addAll(this@replace.subList(index + 1, size))
	}
