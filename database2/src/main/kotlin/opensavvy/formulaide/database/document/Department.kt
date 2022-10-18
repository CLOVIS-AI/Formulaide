package opensavvy.formulaide.database.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.formulaide.core.AbstractDepartments
import opensavvy.state.Slice.Companion.pending
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.State
import opensavvy.state.ensureFound
import opensavvy.state.ensureValid
import opensavvy.state.state
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import opensavvy.formulaide.core.Department as CoreDepartment

@Serializable
internal class Department(
	@SerialName("_id") val id: String,
	val name: String,
	val open: Boolean,
)

class Departments internal constructor(
	private val departments: CoroutineCollection<Department>,
	override val cache: RefCache<CoreDepartment>,
) : AbstractDepartments {

	override fun list(includeClosed: Boolean): State<List<CoreDepartment.Ref>> = state {
		emit(pending(0.0))

		val filter = if (includeClosed)
			null
		else
			Department::open eq true

		val result = departments.find(filter)
			.batchSize(5)
			.toList()
			.map { CoreDepartment.Ref(it.id, this@Departments) }

		emit(successful(result))
	}

	override fun create(name: String): State<CoreDepartment.Ref> = state {
		emit(pending(0.0))

		val id = newId<Department>().toString()
		departments.insertOne(Department(id, name, open = true))

		emit(successful(CoreDepartment.Ref(id, this@Departments)))
	}

	override fun open(department: CoreDepartment.Ref): State<Unit> = state {
		emit(pending(0.0))
		departments.updateOne(Department::id eq department.id, setValue(Department::open, true))
		department.expire()
		emit(successful(Unit))
	}

	override fun close(department: CoreDepartment.Ref): State<Unit> = state {
		emit(pending(0.0))
		departments.updateOne(Department::id eq department.id, setValue(Department::open, false))
		department.expire()
		emit(successful(Unit))
	}

	override fun directRequest(ref: Ref<CoreDepartment>): State<CoreDepartment> = state {
		emit(pending(0.0))
		ensureValid(ref is CoreDepartment.Ref) { "${this@Departments} n'accepte pas la référence $ref" }

		val department = departments.findOne(Department::id eq ref.id)
		ensureFound(department != null) { "Le département demandé n'existe pas : $ref" }

		val result = CoreDepartment(
			name = department.name,
			open = department.open,
		)
		emit(successful(result))
	}
}
