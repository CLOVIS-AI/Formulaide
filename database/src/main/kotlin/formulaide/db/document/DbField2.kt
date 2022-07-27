package formulaide.db.document

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
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.newId

class Fields(
	private val fields: CoroutineCollection<FlatField.Container>,
	override val cache: Cache<FlatField.Container>,
) : FieldBackbone {
	override suspend fun create(name: String, root: Field): FlatField.Container.Ref {
		val id = newId<FlatField.Container>()
			.toString()
		fields.insertOne(
			FlatField.Container(
				id,
				name,
				root.flatten(this)
			)
		)
		return FlatField.Container.Ref(id, this)
	}

	override fun directRequest(ref: Ref<FlatField.Container>): Flow<Data<FlatField.Container>> {
		require(ref is FlatField.Container.Ref) { "$this doesn't support the reference $ref" }

		return flow {
			val result =
				fields.findOne(FlatField.Container::id eq ref.id) ?: error("Le champ ${ref.id} est introuvable")
			emit(Data(Result.Success(result), Data.Status.Completed, ref))
		}
	}
}
