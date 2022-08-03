package formulaide.client.bones

import formulaide.api.bones.ApiNewFields
import formulaide.client.Client
import formulaide.core.field.Field
import formulaide.core.field.FieldBackbone
import formulaide.core.field.FlatField
import formulaide.core.field.flatten
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref
import opensavvy.backbone.Result

class Fields(
	private val client: Client,
	override val cache: Cache<FlatField.Container>,
) : FieldBackbone {
	override suspend fun create(name: String, root: Field): FlatField.Container.Ref =
		client.post(
			"/api/schema/fields", ApiNewFields(
				name,
				root.flatten(this)
			)
		)

	override fun directRequest(ref: Ref<FlatField.Container>): Flow<Data<FlatField.Container>> {
		require(ref is FlatField.Container.Ref) { "$this doesn't support the reference $ref" }

		return flow {
			val field: FlatField.Container = client.get("/api/schema/fields/${ref.id}")
			emit(Data(Result.Success(field), Data.Status.Completed, ref))
		}
	}
}
