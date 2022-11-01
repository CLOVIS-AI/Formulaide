package formulaide.client.bones

import formulaide.api.bones.ApiNewFields
import formulaide.client.Client
import formulaide.core.field.Field
import formulaide.core.field.FieldBackbone
import formulaide.core.field.FlatField
import formulaide.core.field.flatten
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.state.slice.Slice
import opensavvy.state.slice.ensureValid
import opensavvy.state.slice.slice

class Fields(
	private val client: Client,
	override val cache: RefCache<FlatField.Container>,
) : FieldBackbone {
	override suspend fun create(name: String, root: Field): FlatField.Container.Ref =
		client.post(
			"/api/schema/fields", ApiNewFields(
				name,
				root.flatten(this)
			)
		)

	override suspend fun directRequest(ref: Ref<FlatField.Container>): Slice<FlatField.Container> = slice {
		ensureValid(ref is FlatField.Container.Ref) { "${this@Fields} doesn't support the reference $ref" }

		val field: FlatField.Container = client.get("/api/schema/fields/${ref.id}")

		field
	}
}
