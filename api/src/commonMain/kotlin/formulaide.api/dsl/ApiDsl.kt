package formulaide.api.dsl

import formulaide.api.fields.Field

@DslMarker
annotation class ApiDsl

@ApiDsl
class FieldDsl<F: Field>(
	private var lastId: Int = 0,
	internal val fields: MutableList<F> = ArrayList(5)
) {

	internal fun nextInfo(): Pair<String, Int> = lastId++
		.let { it.toString() to it }
}
