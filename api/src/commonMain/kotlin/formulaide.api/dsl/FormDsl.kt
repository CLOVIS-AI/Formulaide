package formulaide.api.dsl

import formulaide.api.data.Action
import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.fields.*
import formulaide.api.types.Arity
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef

typealias FormDsl<F> = FieldDsl<F>

/**
 * DSL builder to instantiate a [Form].
 */
fun form(
	name: String,
	public: Boolean,
	mainFields: FormRoot,
	vararg actions: Action
) = Form(
	Ref.SPECIAL_TOKEN_NEW,
	name,
	open = true,
	public,
	mainFields,
	actions = actions.asList()
)

/**
 * DSL builder to instantiate a [FormRoot].
 */
fun formRoot(
	composites: List<Composite>,
	fields: FormDsl<ShallowFormField>.() -> Unit,
): FormRoot {
	val dsl = FormDsl<ShallowFormField>()
	dsl.fields()

	return FormRoot(
		dsl.fields
	).also { it.validate(composites) }
}

fun formRoot(
	vararg composites: Composite,
	fields: FormDsl<ShallowFormField>.() -> Unit,
) = formRoot(composites.asList(), fields)

fun FieldDsl<ShallowFormField>.simple(
	name: String,
	simple: SimpleField,
): ShallowFormField.Simple {
	val (id, order) = nextInfo()

	return ShallowFormField.Simple(id, order, name, simple).also { fields += it }
}

fun FieldDsl<ShallowFormField>.union(
	name: String,
	arity: Arity,
	options: FormDsl<ShallowFormField>.() -> Unit,
): ShallowFormField.Union {
	val (id, order) = nextInfo()

	val dsl = FormDsl<ShallowFormField>()
	dsl.options()

	return ShallowFormField.Union(id, order, name, arity, dsl.fields).also { fields += it }
}

fun FieldDsl<ShallowFormField>.composite(
	name: String,
	arity: Arity,
	composite: Ref<Composite>,
	fields: FormDsl<DeepFormField>.() -> Unit,
): ShallowFormField.Composite {
	val (id, order) = nextInfo()

	val dsl = FormDsl<DeepFormField>()
	dsl.fields()

	return ShallowFormField.Composite(id, order, name, arity, composite, dsl.fields)
		.also { this.fields += it }
}

fun FieldDsl<ShallowFormField>.composite(
	name: String,
	arity: Arity,
	composite: Composite,
	fields: FormDsl<DeepFormField>.() -> Unit,
) = this.composite(name, arity, composite.createRef(), fields)

fun FieldDsl<DeepFormField>.simple(
	field: DataField.Simple,
	simple: SimpleField,
) = DeepFormField.Simple(field.createRef(), simple).also { fields += it }

fun FieldDsl<DeepFormField>.union(
	field: DataField.Union,
	arity: Arity,
	options: FormDsl<DeepFormField>.() -> Unit,
): DeepFormField.Union {
	val dsl = FormDsl<DeepFormField>()
	dsl.options()

	return DeepFormField.Union(field, arity, dsl.fields).also { fields += it }
}

fun FieldDsl<DeepFormField>.composite(
	field: DataField.Composite,
	arity: Arity,
	fields: FormDsl<DeepFormField>.() -> Unit,
): DeepFormField.Composite {
	val dsl = FormDsl<DeepFormField>()
	dsl.fields()

	return DeepFormField.Composite(field, arity, dsl.fields).also { this.fields += it }
}
