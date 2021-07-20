package formulaide.api.dsl

import formulaide.api.data.Composite
import formulaide.api.data.SPECIAL_TOKEN_RECURSION
import formulaide.api.fields.DataField
import formulaide.api.fields.SimpleField
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef

typealias CompositeDsl<F> = FieldDsl<F>

/**
 * DSL builder to instantiate a [Composite].
 */
fun composite(
	name: String,
	composites: List<Composite>,
	fields: CompositeDsl<DataField>.() -> Unit
) : Composite {
	val dsl = CompositeDsl<DataField>()
	dsl.fields()

	return Composite(
		Ref.SPECIAL_TOKEN_NEW,
		name,
		dsl.fields
	).also { it.loadAllowRecursive(composites); it.validate() }
}

fun composite(
	name: String,
	vararg composites: Composite,
	fields: CompositeDsl<DataField>.() -> Unit,
) = composite(name, composites.asList(), fields)

fun CompositeDsl<DataField>.simple(
	name: String,
	field: SimpleField
): DataField.Simple {
	val (id, order) = nextInfo()

	return DataField.Simple(id, order, name, field).also { fields += it }
}

fun CompositeDsl<DataField>.union(
	name: String,
	arity: Arity,
	options: CompositeDsl<DataField>.() -> Unit,
) : DataField.Union {
	val dsl = CompositeDsl<DataField>()
	dsl.options()

	val (id, order) = nextInfo()

	return DataField.Union(
		id,
		order,
		name,
		arity,
		dsl.fields
	).also { fields += it }
}

fun CompositeDsl<DataField>.composite(
	name: String,
	arity: Arity,
	composite: Ref<Composite>
) : DataField.Composite {
	val (id, order) = nextInfo()

	return DataField.Composite(id, order, name, arity, composite).also { fields += it }
}

fun CompositeDsl<DataField>.composite(
	name: String,
	arity: Arity,
	composite: Composite
) = this.composite(name, arity, composite.createRef())

fun CompositeDsl<DataField>.compositeItself(
	name: String,
	arity: Arity
) = this.composite(name, arity, Ref(SPECIAL_TOKEN_RECURSION))
