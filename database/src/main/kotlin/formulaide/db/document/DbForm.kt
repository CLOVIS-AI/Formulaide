package formulaide.db.document

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.data.FormMetadata
import formulaide.api.fields.DeepFormField
import formulaide.api.fields.ShallowFormField
import formulaide.api.fields.asSequence
import formulaide.api.types.Ref.Companion.load
import formulaide.api.types.ReferenceId
import formulaide.db.Database
import formulaide.db.utils.generateId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.litote.kmongo.eq
import org.litote.kmongo.match

/**
 * Gets the list of forms.
 *
 * @param public If `true`, searches for public forms. If `false`, searches for internal forms.
 * If `null`, searches for all forms without regards for their visibility. See [Form.public].
 * @param open If `true`, searches for open forms. If `false`, searches for closed.
 * If `null`, searches for all forms without regards for their status. See [Form.open].
 */
suspend fun Database.listForms(public: Boolean?, open: Boolean? = true): List<Form> {
	val matchPublic =
		if (public != null) match(Form::public eq public)
		else null

	val matchOpen =
		if (open != null) match(Form::open eq open)
		else null

	val pipeline = arrayOf(matchPublic, matchOpen)
		.filterNotNull()

	return forms.aggregate<Form>(pipeline).toList()
}

suspend fun Database.findForm(id: ReferenceId) =
	forms.findOne(Form::id eq id)

suspend fun Database.createForm(form: Form): Form {
	require(form.open) { "Il est interdit de créer un formulaire fermé" }

	form.load(listComposites())
	form.validate()

	val newForm = form.copy(id = generateId<Form>())
	forms.insertOne(newForm)
	return newForm
}

suspend fun Database.referencedComposites(form: Form): List<Composite> {
	form.mainFields.load(listComposites())

	val compositeIds = form.mainFields
		.asSequence()
		.flatMap { field ->
			when (field) {
				is ShallowFormField.Composite -> sequenceOf(field.ref.id)
				is DeepFormField.Composite -> sequenceOf(field.dataField.ref.id)
				else -> emptySequence()
			}
		}
		.toHashSet()

	return coroutineScope {
		compositeIds.map {
			async { findComposite(it) ?: error("La donnée composée $it est introuvable") }
		}.awaitAll()
	}
}

suspend fun Database.editForm(edition: FormMetadata): Form {
	edition.form.load { findForm(it) ?: error("Le formulaire '$it' est introuvable") }

	val form = edition.form.obj

	val new = form.copy(
		public = edition.public ?: form.public,
		open = edition.open ?: form.open,
		mainFields = edition.mainFields ?: form.mainFields,
		actions = edition.actions ?: form.actions,
	)

	forms.replaceOne(Form::id eq form.id, new)

	return new
}
