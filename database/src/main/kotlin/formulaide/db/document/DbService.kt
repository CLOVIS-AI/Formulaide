package formulaide.db.document

import formulaide.api.users.Service
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.State
import opensavvy.state.ensureFound
import opensavvy.state.ensureValid
import opensavvy.state.state
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
	override suspend fun all(includeClosed: Boolean): List<Department.Ref> {
		return services.find(
			(DbService::open eq true).takeIf { !includeClosed }
		)
			.toList()
			.map { Department.Ref(it.id.toString(), this) }
	}

	override suspend fun create(name: String): Department.Ref {
		var id: Int
		do {
			id = Random.nextInt()
		} while (services.findOne(DbService::id eq id) != null)

		services.insertOne(DbService(name, id, true))
		return Department.Ref(id.toString(), this)
	}

	override suspend fun open(department: Department.Ref) {
		services.updateOne(DbService::id eq department.id.toInt(), setValue(DbService::open, true))
		department.expire()
	}

	override suspend fun close(department: Department.Ref) {
		services.updateOne(DbService::id eq department.id.toInt(), setValue(DbService::open, false))
		department.expire()
	}

	fun fromId(id: Int) = Department.Ref(id.toString(), this)

	override fun directRequest(ref: Ref<Department>): State<Department> = state {
		ensureValid(ref is Department.Ref) { "${this@Departments} doesn't support the reference $ref" }

		val dbService = services.findOne(DbService::id eq ref.id.toInt())
		ensureFound(dbService != null) { "Le département demandé n'existe pas : $ref" }

		val department = Department(
			dbService.id.toString(),
			dbService.name,
			dbService.open,
		)
		emit(successful(department))
	}
}
