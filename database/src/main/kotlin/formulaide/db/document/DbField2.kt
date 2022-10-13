package formulaide.db.document

import formulaide.core.field.Field
import formulaide.core.field.FieldBackbone
import formulaide.core.field.FlatField
import formulaide.core.field.flatten
import opensavvy.backbone.Ref
import opensavvy.backbone.RefState
import opensavvy.cache.Cache
import opensavvy.state.emitSuccessful
import opensavvy.state.ensureFound
import opensavvy.state.ensureValid
import opensavvy.state.state
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.newId

class Fields(
	private val fields: CoroutineCollection<FlatField.Container>,
	override val cache: Cache<Ref<FlatField.Container>, FlatField.Container>,
) : FieldBackbone {
	override suspend fun create(name: String, root: Field): FlatField.Container.Ref {
		val id = newId<FlatField.Container>()
			.toString()
		fields.insertOne(
			FlatField.Container(
				id,
				root.flatten(this)
			)
		)
		return FlatField.Container.Ref(id, this)
	}

	fun fromId(id: String) = FlatField.Container.Ref(id, this)

	override fun directRequest(ref: Ref<FlatField.Container>): RefState<FlatField.Container> = state {
		ensureValid(ref, ref is FlatField.Container.Ref) { "${this@Fields} doesn't support the reference $ref" }

		val result = fields.findOne(FlatField.Container::id eq ref.id)
		ensureFound(ref, result != null) { "Le champ $ref est introuvable" }

		emitSuccessful(ref, result)
	}
}
