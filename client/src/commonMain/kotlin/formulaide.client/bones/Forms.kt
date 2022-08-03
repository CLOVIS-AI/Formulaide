package formulaide.client.bones

import formulaide.api.bones.ApiFormEdition
import formulaide.api.bones.ApiNewForm
import formulaide.client.Client
import formulaide.core.form.Form
import formulaide.core.form.FormBackbone
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref
import opensavvy.backbone.Result

class Forms(
	private val client: Client,
	override val cache: Cache<Form>,
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

	override fun directRequest(ref: Ref<Form>): Flow<Data<Form>> {
		require(ref is Form.Ref) { "$this doesn't support the reference $ref" }

		return flow {
			val result: Form = client.get("/api/schema/forms/${ref.id}")
			emit(Data(Result.Success(result), Data.Status.Completed, ref))
		}
	}
}
