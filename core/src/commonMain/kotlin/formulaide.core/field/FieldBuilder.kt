package formulaide.core.field

fun label(text: String) = Field.Label(text)

fun labelFrom(sourceId: Field.Id, from: Field.Container, text: String? = null): Field.Label {
	val source = from[sourceId] ?: error("L'identifiant $sourceId ne fait référence à aucun champ")
	require(source is Field.Label) { "L'identifiant $sourceId fait référence à un champ du mauvais type : $source" }
	return Field.Label(
		text ?: source.label,
		from to sourceId
	)
}

fun input(label: String, input: InputConstraints) = Field.Input(
	label,
	input,
)

fun inputFrom(
	sourceId: Field.Id,
	from: Field.Container,
	label: String? = null,
	input: InputConstraints? = null,
): Field.Input {
	val source = from[sourceId] ?: error("L'identifiant $sourceId ne fait référence à aucun champ")
	require(source is Field.Input) { "L'identifiant $sourceId fait référence à un champ du mauvais type : $source" }
	return Field.Input(
		label ?: source.label, input ?: source.input, from to sourceId
	)
}

fun choice(label: String, vararg options: Pair<LocalFieldId, Field>) = Field.Choice(
	label, mapOf(*options)
)

fun choiceFrom(
	sourceId: Field.Id,
	from: Field.Container,
	vararg choice: Pair<LocalFieldId, Field>,
	label: String? = null,
): Field.Choice {
	val source = from[sourceId] ?: error("L'identifiant $sourceId ne fait référence à aucun champ")
	require(source is Field.Choice) { "L'identifiant $sourceId fait référence à un champ du mauvais type : $source" }
	return Field.Choice(
		label ?: source.label,
		choice.takeIf { it.isNotEmpty() }?.let { mapOf(*it) } ?: source.indexedFields,
		from to sourceId
	)
}

fun group(label: String, vararg fields: Pair<LocalFieldId, Field>) = Field.Group(
	label, mapOf(*fields)
)

fun groupFrom(
	sourceId: Field.Id,
	from: Field.Container,
	vararg fields: Pair<LocalFieldId, Field>,
	label: String? = null,
): Field.Group {
	val source = from[sourceId] ?: error("L'identifiant $sourceId ne fait référence à aucun champ")
	require(source is Field.Group) { "L'identifiant $sourceId fait référence à un champ du mauvais type : $source" }
	return Field.Group(
		label ?: source.label,
		fields.takeIf { it.isNotEmpty() }?.let { mapOf(*it) } ?: source.indexedFields,
		from to sourceId
	)
}

fun list(label: String, allowed: UIntRange, field: Field) = Field.List(
	label, field, allowed
)

fun listFrom(
	sourceId: Field.Id,
	from: Field.Container,
	label: String? = null,
	allowed: UIntRange? = null,
	field: Field? = null,
): Field.List {
	val source = from[sourceId] ?: error("L'identifiant $sourceId ne fait référence à aucun champ")
	require(source is Field.List) { "L'identifiant $sourceId fait référence à un champ du mauvais type : $source" }
	return Field.List(
		label ?: source.label, field ?: source.field, allowed ?: source.allowed, from to sourceId
	)
}
