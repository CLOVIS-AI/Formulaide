package formulaide.db.document

import formulaide.core.form.Template
import formulaide.core.form.TemplateBackbone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Result
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class Templates(
	val templates: CoroutineCollection<Template>,
	override val cache: Cache<Template>,
) : TemplateBackbone {
	override suspend fun all(): List<Template.Ref> = templates.find()
		.toList()
		.map { template ->
			Template.Ref(template.id, this)
				.also { cache.update(it, template) }
		}

	override suspend fun create(name: String, firstVersion: Template.Version): Template.Ref {
		val id = newId<Template>().toString()
		val template = Template(
			id,
			name,
			listOf(firstVersion)
		)

		templates.insertOne(template)

		return Template.Ref(id, this)
	}

	override suspend fun createVersion(template: Template.Ref, version: Template.Version) {
		templates.updateOne(Template::id eq template.id, push(Template::versions, version))
		template.expire()
	}

	override suspend fun edit(template: Template.Ref, name: String?) {
		val updates = mutableListOf<Bson>()

		if (name != null)
			updates.add(setValue(Template::name, name))

		templates.updateOne(Template::id eq template.id, combine(updates))
		template.expire()
	}

	override fun directRequest(ref: Ref<Template>): Flow<Data<Template>> {
		require(ref is Template.Ref) { "$this doesn't support the reference $ref" }

		return flow {
			val value = templates.findOne(Template::id eq ref.id) ?: error("Le modèle ${ref.id} est introuvable")
			emit(Data(Result.Success(value), Data.Status.Completed, ref))
		}
	}

	fun fromId(id: String) = Template.Ref(id, this)
}
