package formulaide.db.document

import formulaide.core.form.Template
import formulaide.core.form.TemplateBackbone
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefState
import opensavvy.cache.Cache
import opensavvy.state.emitSuccessful
import opensavvy.state.ensureFound
import opensavvy.state.ensureValid
import opensavvy.state.state
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection

class Templates(
	val templates: CoroutineCollection<Template>,
	override val cache: Cache<Ref<Template>, Template>,
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

	override fun directRequest(ref: Ref<Template>): RefState<Template> = state {
		ensureValid(ref, ref is Template.Ref) { "${this@Templates} doesn't support the reference $ref" }

		val value = templates.findOne(Template::id eq ref.id)
		ensureFound(ref, value != null) { "Le modèle ${ref.id} est introuvable" }

		emitSuccessful(ref, value)
	}

	fun fromId(id: String) = Template.Ref(id, this)
}
