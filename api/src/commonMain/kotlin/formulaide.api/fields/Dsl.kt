package formulaide.api.fields

import formulaide.api.types.Arity

@DslMarker
annotation class FieldDslMarker

//region Unified DSL

@FieldDslMarker
class FieldDsl<F: Field.Contained>(internal var order: Int, internal val fields: MutableList<F>)

fun FieldDsl<FlatField>.simple(field: SimpleField) {
	val id = order++
	fields += FlatField.Simple(id, id, field)
}

fun FieldDsl<DeepField>.simple(field: SimpleField) {
	val id = order++
	fields += DeepField.Simple(id, id, field)
}

fun FieldDsl<FlatField>.composite(name: String, arity: Arity, composite: Composite) {
	val id = order++
	fields += FlatField.Container(id, id, name, arity, composite.fields)
}

fun FieldDsl<DeepField>.composite(name: String, arity: Arity, composite: Composite, block: FieldDsl<DeepField>.() -> Unit) {
	val dsl = FieldDsl<DeepField>(0, mutableListOf())

	val id = order++
	fields += DeepField.Container(id, id, name, arity, composite.fields, dsl.fields)
}

fun FieldDsl<FlatField>.union(name: String, arity: Arity, block: FieldDsl<FlatField>.() -> Unit) {
	val dsl = FieldDsl<FlatField>(0, mutableListOf())

	dsl.block()

	val id = order++
	fields += FlatField.Union(id, id, name, arity, dsl.fields)
}

fun FieldDsl<DeepField>.union(name: String, arity: Arity, block: FieldDsl<DeepField>.() -> Unit) {
	val dsl = FieldDsl<DeepField>(0, mutableListOf())

	dsl.block()

	val id = order++
	fields += DeepField.Union(id, id, name, arity, dsl.fields)
}

//endregion

fun <F: Field.Contained> fields(block: FieldDsl<F>.() -> Unit): Field.Container.TopLevel<F> {
	val dsl = FieldDsl<F>(0, mutableListOf())
	dsl.block()
	return Field.Container.TopLevel(dsl.order++, dsl.fields)
}
