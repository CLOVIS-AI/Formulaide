package formulaide.db.document

import formulaide.core.form.Form
import formulaide.core.form.FormBackbone
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.state.slice.Slice
import opensavvy.state.slice.ensureFound
import opensavvy.state.slice.ensureValid
import opensavvy.state.slice.slice
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class Forms(
	val forms: CoroutineCollection<Form>,
	override val cache: RefCache<Form>,
) : FormBackbone {
	override suspend fun all(includeClosed: Boolean): List<Form.Ref> {
		val forms = forms.find(
			(Form::open eq true).takeIf { !includeClosed }
		)

		return forms
			.toList()
			.map { form ->
				Form.Ref(form.id, this)
					.also { cache.update(it, form) }
			}
	}

	override suspend fun create(name: String, firstVersion: Form.Version, public: Boolean): Form.Ref {
		val id = newId<Form>().toString()
		val form = Form(
			id,
			name,
			listOf(firstVersion),
			public,
			open = true,
		)
		forms.insertOne(form)
		return Form.Ref(id, this)
	}

	override suspend fun createVersion(form: Form.Ref, new: Form.Version) {
		forms.updateOne(Form::id eq form.id, push(Form::versions, new))
		form.expire()
	}

	override suspend fun edit(form: Form.Ref, name: String?, public: Boolean?, open: Boolean?) {
		val updates = mutableListOf<Bson>()

		if (name != null)
			updates.add(setValue(Form::name, name))

		if (public != null)
			updates.add(setValue(Form::public, public))

		if (open != null)
			updates.add(setValue(Form::open, open))

		forms.updateOne(Form::id eq form.id, combine(updates))
		form.expire()
	}

	fun fromId(id: String) = Form.Ref(id, this)

	override suspend fun directRequest(ref: Ref<Form>): Slice<Form> = slice {
		ensureValid(ref is Form.Ref) { "${this@Forms} doesn't support the reference $ref" }

		val result = forms.findOne(Form::id eq ref.id)
		ensureFound(result != null) { "Le formulaire ${MongoOperator.id} est introuvable" }

		result
	}
}
