package formulaide.client.bones

import formulaide.api.bones.ApiNewTemplate
import formulaide.api.bones.ApiTemplateEdition
import formulaide.client.Client
import formulaide.core.form.Template
import formulaide.core.form.TemplateBackbone
import opensavvy.backbone.Ref
import opensavvy.cache.Cache
import opensavvy.state.slice.Slice
import opensavvy.state.slice.ensureValid
import opensavvy.state.slice.slice

class Templates(
	private val client: Client,
	override val cache: Cache<Ref<Template>, Template>,
) : TemplateBackbone {
	override suspend fun all(): List<Template.Ref> =
		client.get("/api/schema/templates")

	override suspend fun create(name: String, firstVersion: Template.Version): Template.Ref =
		client.post(
			"/api/schema/templates", ApiNewTemplate(
				name,
				firstVersion
			)
		)

	override suspend fun createVersion(template: Template.Ref, version: Template.Version) {
		client.post<Template.Ref>("/api/schema/templates/${template.id}/version", version)
	}

	override suspend fun edit(template: Template.Ref, name: String?) {
		client.patch<Template.Ref>(
			"/api/schema/templates/${template.id}", ApiTemplateEdition(
				name
			)
		)
	}

	override suspend fun directRequest(ref: Ref<Template>): Slice<Template> = slice {
		ensureValid(ref is Template.Ref) { "${this@Templates} doesn't support the reference $ref" }

		val result: Template = client.get("/api/schema/templates/${ref.id}")

		result
	}
}
