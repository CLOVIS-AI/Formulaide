package formulaide.client.bones

import formulaide.api.bones.ApiNewTemplate
import formulaide.api.bones.ApiTemplateEdition
import formulaide.client.Client
import formulaide.core.form.Template
import formulaide.core.form.TemplateBackbone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref
import opensavvy.backbone.Result

class Templates(
	private val client: Client,
	override val cache: Cache<Template>,
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

	override fun directRequest(ref: Ref<Template>): Flow<Data<Template>> {
		require(ref is Template.Ref) { "$this doesn't support the reference $ref" }

		return flow {
			val result: Template = client.get("/api/schema/templates/${ref.id}")
			emit(Data(Result.Success(result), Data.Status.Completed, ref))
		}
	}
}
