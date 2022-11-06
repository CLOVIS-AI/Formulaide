package opensavvy.formulaide.database.document

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.formulaide.core.AbstractDepartments
import opensavvy.state.Progression.Companion.loading
import opensavvy.state.ProgressionReporter.Companion.report
import opensavvy.state.slice.*
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import opensavvy.formulaide.core.Department as CoreDepartment

@Serializable
internal class Department(
	@SerialName("_id") val id: String,
	val name: String = "Nom manquant",
	val open: Boolean = true,
)

class Departments internal constructor(
	private val departments: CoroutineCollection<Department>,
	override val cache: RefCache<CoreDepartment>,
) : AbstractDepartments {

	override suspend fun list(includeClosed: Boolean): Slice<List<CoreDepartment.Ref>> = slice {
		report(loading(0.0))

		val filter = if (includeClosed)
			null
		else
			Department::open eq true

		val result = departments.find(filter)
			.batchSize(5)
			.projection(Department::id)
			.toList()
			.map { CoreDepartment.Ref(it.id, this@Departments) }

		result
	}

	override suspend fun create(name: String): Slice<CoreDepartment.Ref> = slice {
		report(loading(0.0))

		val id = newId<Department>().toString()
		departments.insertOne(Department(id, name, open = true))

		CoreDepartment.Ref(id, this@Departments)
	}

	override suspend fun open(department: CoreDepartment.Ref): Slice<Unit> = slice {
		report(loading(0.0))
		departments.updateOne(Department::id eq department.id, setValue(Department::open, true))
		department.expire()
	}

	override suspend fun close(department: CoreDepartment.Ref): Slice<Unit> = slice {
		report(loading(0.0))
		departments.updateOne(Department::id eq department.id, setValue(Department::open, false))
		department.expire()
	}

	override suspend fun directRequest(ref: Ref<CoreDepartment>): Slice<CoreDepartment> = slice {
		report(loading(0.0))
		ensureValid(ref is CoreDepartment.Ref) { "${this@Departments} n'accepte pas la référence $ref" }

		val department = departments.findOne(Department::id eq ref.id)
		ensureFound(department != null) { "Le département demandé n'existe pas : $ref" }

		val result = CoreDepartment(
			name = department.name,
			open = department.open,
		)
		result
	}
}
