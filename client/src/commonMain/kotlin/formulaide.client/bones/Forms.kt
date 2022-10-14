package formulaide.client.bones

import formulaide.api.bones.ApiFormEdition
import formulaide.api.bones.ApiNewForm
import formulaide.client.Client
import formulaide.core.form.Form
import formulaide.core.form.FormBackbone
import io.ktor.client.request.*
import opensavvy.backbone.Ref
import opensavvy.cache.Cache
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.state

class Forms(
	private val client: Client,
	override val cache: Cache<Ref<Form>, Form>,
) : FormBackbone {
	override suspend fun all(includeClosed: Boolean): List<Form.Ref> =
		client.get("/api/schema/forms") {
			parameter("includeClosed", includeClosed)
		}

	override suspend fun create(name: String, firstVersion: Form.Version, public: Boolean): Form.Ref =
		client.post(
			"/api/schema/forms", ApiNewForm(
				name,
				firstVersion,
				public,
			)
		)

	override suspend fun createVersion(form: Form.Ref, new: Form.Version) {
		client.post<Form.Ref>("/api/schema/forms/${form.id}/version", new)
	}

	override suspend fun edit(form: Form.Ref, name: String?, public: Boolean?, open: Boolean?) {
		client.patch<Form.Ref>(
			"/api/schema/forms/${form.id}", ApiFormEdition(
				name,
				public,
				open,
			)
		)
	}

	override fun directRequest(ref: Ref<Form>): State<Form> = state {
		ensureValid(ref is Form.Ref) { "${this@Forms} doesn't support the reference $ref" }

		val result: Form = client.get("/api/schema/forms/${ref.id}")

		emit(successful(result))
	}
}
