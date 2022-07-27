package formulaide.db.document

import formulaide.core.form.Form
import formulaide.core.form.FormBackbone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Result
import org.bson.conversions.Bson
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.newId
import org.litote.kmongo.push
import org.litote.kmongo.setValue

class Forms(
	val forms: CoroutineCollection<Form>,
	override val cache: Cache<Form>,
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

		forms.updateOne(Form::id eq form.id, org.litote.kmongo.combine(updates))
		form.expire()
	}

	override fun directRequest(ref: Ref<Form>): Flow<Data<Form>> {
		require(ref is Form.Ref) { "$this doesn't support the reference $ref" }

		return flow {
			val result = forms.findOne(Form::id eq ref.id) ?: error("Le formulaire ${ref.id} est introuvable")
			emit(Data(Result.Success(result), Data.Status.Completed, ref))
		}
	}
}
