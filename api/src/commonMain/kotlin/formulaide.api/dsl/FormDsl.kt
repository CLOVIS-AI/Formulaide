package formulaide.api.dsl

import formulaide.api.data.Action
import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.fields.DataField
import formulaide.api.fields.FormField
import formulaide.api.fields.FormRoot
import formulaide.api.fields.SimpleField
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
	fields: FormDsl<FormField.Shallow>.() -> Unit
): FormRoot {
	val dsl = FormDsl<FormField.Shallow>()
	dsl.fields()

	return FormRoot(
		dsl.fields
	).also { it.validate(composites) }
}

fun formRoot(
	vararg composites: Composite,
	fields: FormDsl<FormField.Shallow>.() -> Unit
) = formRoot(composites.asList(), fields)

fun FieldDsl<FormField.Shallow>.simple(
	name: String,
	simple: SimpleField
) : FormField.Shallow.Simple {
	val (id, order) = nextInfo()

	return FormField.Shallow.Simple(id, order, name, simple).also { fields += it }
}

fun FieldDsl<FormField.Shallow>.union(
	name: String,
	arity: Arity,
	options: FormDsl<FormField.Shallow>.() -> Unit
) : FormField.Shallow.Union {
	val (id, order) = nextInfo()

	val dsl = FormDsl<FormField.Shallow>()
	dsl.options()

	return FormField.Shallow.Union(id, order, name, arity, dsl.fields).also { fields += it }
}

fun FieldDsl<FormField.Shallow>.composite(
	name: String,
	arity: Arity,
	composite: Ref<Composite>,
	fields: FormDsl<FormField.Deep>.() -> Unit
) : FormField.Shallow.Composite {
	val (id, order) = nextInfo()

	val dsl = FormDsl<FormField.Deep>()
	dsl.fields()

	return FormField.Shallow.Composite(id, order, name, arity, composite, dsl.fields).also { this.fields += it }
}

fun FieldDsl<FormField.Shallow>.composite(
	name: String,
	arity: Arity,
	composite: Composite,
	fields: FormDsl<FormField.Deep>.() -> Unit
) = this.composite(name, arity, composite.createRef(), fields)

fun FieldDsl<FormField.Deep>.simple(
	field: DataField.Simple,
	simple: SimpleField
) = FormField.Deep.Simple(field.createRef(), simple).also { fields += it }

fun FieldDsl<FormField.Deep>.union(
	field: DataField.Union,
	arity: Arity,
	options: FormDsl<FormField.Deep>.() -> Unit,
): FormField.Deep.Union {
	val dsl = FormDsl<FormField.Deep>()
	dsl.options()

	return FormField.Deep.Union(field, arity, dsl.fields).also { fields += it }
}

fun FieldDsl<FormField.Deep>.composite(
	field: DataField.Composite,
	arity: Arity,
	fields: FormDsl<FormField.Deep>.() -> Unit,
): FormField.Deep.Composite {
	val dsl = FormDsl<FormField.Deep>()
	dsl.fields()

	return FormField.Deep.Composite(field, arity, dsl.fields).also { this.fields += it }
}
