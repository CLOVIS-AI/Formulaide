package formulaide.client.bones

import formulaide.api.bones.ApiNewFields
import formulaide.client.Client
import formulaide.core.field.Field
import formulaide.core.field.FieldBackbone
import formulaide.core.field.FlatField
import formulaide.core.field.flatten
import opensavvy.backbone.Ref
import opensavvy.backbone.RefState
import opensavvy.cache.Cache
import opensavvy.state.emitSuccessful
import opensavvy.state.ensureValid
import opensavvy.state.state

class Fields(
	private val client: Client,
	override val cache: Cache<Ref<FlatField.Container>, FlatField.Container>,
) : FieldBackbone {
	override suspend fun create(name: String, root: Field): FlatField.Container.Ref =
		client.post(
			"/api/schema/fields", ApiNewFields(
				name,
				root.flatten(this)
			)
		)

	override fun directRequest(ref: Ref<FlatField.Container>): RefState<FlatField.Container> = state {
		ensureValid(ref, ref is FlatField.Container.Ref) { "${this@Fields} doesn't support the reference $ref" }

		val field: FlatField.Container = client.get("/api/schema/fields/${ref.id}")

		emitSuccessful(ref, field)
	}
}
