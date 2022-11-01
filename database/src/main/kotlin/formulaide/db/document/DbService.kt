package formulaide.db.document

import formulaide.api.users.Service
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.spine.Id
import opensavvy.state.slice.*
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
import kotlin.random.Random

/**
 * Id of a [DbService].
 */
typealias DbServiceId = Int

/**
 * Database representation of a [Service].
 *
 * @property open Whether this service is currently opened or not. Only the administrator can see closed services.
 */
@Serializable
data class DbService(
	val name: String,
	val id: DbServiceId,
	val open: Boolean,
)

class Departments(
	private val services: CoroutineCollection<DbService>,
	override val cache: RefCache<Department>,
) : DepartmentBackbone {
	override fun all(includeClosed: Boolean): Flow<Slice<List<Department.Ref>>> = flow {
		val filter = (DbService::open eq true).takeIf { !includeClosed }

		val result = services.find(filter)
			.toList()
			.map { Department.Ref(it.id.toString(), this@Departments) }

		emit(successful(result))
	}

	override fun create(name: String) = flow {
		var id: Int
		do {
			id = Random.nextInt()
		} while (services.findOne(DbService::id eq id) != null)

		services.insertOne(DbService(name, id, true))

		emit(successful(Department.Ref(id.toString(), this@Departments)))
	}

	override fun open(department: Department.Ref) = flow<Slice<Unit>> {
		services.updateOne(DbService::id eq department.id.toInt(), setValue(DbService::open, true))
		department.expire()
		emit(successful(Unit))
	}

	override fun close(department: Department.Ref) = flow<Slice<Unit>> {
		services.updateOne(DbService::id eq department.id.toInt(), setValue(DbService::open, false))
		department.expire()
		emit(successful(Unit))
	}

	fun fromId(id: Int) = Department.Ref(id.toString(), this)

	fun fromId(id: Id) = Department.Ref(id.resource.segments[1].segment, this)

	override suspend fun directRequest(ref: Ref<Department>): Slice<Department> = slice {
		ensureValid(ref is Department.Ref) { "${this@Departments} doesn't support the reference $ref" }

		val dbService = services.findOne(DbService::id eq ref.id.toInt())
		ensureFound(dbService != null) { "Le département demandé n'existe pas : $ref" }

		val department = Department(
			dbService.id.toString(),
			dbService.name,
			dbService.open,
		)
		department
	}
}
