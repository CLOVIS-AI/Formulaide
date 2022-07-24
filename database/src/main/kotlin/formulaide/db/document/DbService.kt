package formulaide.db.document

import formulaide.api.users.Service
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import formulaide.core.Ref
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Result
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
	override val cache: Cache<Department>,
) : DepartmentBackbone {
	override suspend fun all(includeClosed: Boolean): List<Ref<Department>> {
		return services.find(
			(DbService::open eq true).takeIf { !includeClosed }
		)
			.toList()
			.map { Ref(it.id.toString(), this) }
	}

	override suspend fun create(name: String): Ref<Department> {
		var id: Int
		do {
			id = Random.nextInt()
		} while (services.findOne(DbService::id eq id) != null)

		services.insertOne(DbService(name, id, true))
		return Ref(id.toString(), this)
	}

	override suspend fun open(department: Ref<Department>) {
		services.updateOne(DbService::id eq department.id.toInt(), setValue(DbService::open, true))
		department.expire()
	}

	override suspend fun close(department: Ref<Department>) {
		services.updateOne(DbService::id eq department.id.toInt(), setValue(DbService::open, false))
		department.expire()
	}

	fun fromId(id: Int) = Ref(id.toString(), this)

	override fun directRequest(ref: opensavvy.backbone.Ref<Department>): Flow<Data<Department>> = flow {
		require(ref is Ref) { "$this doesn't support the reference $ref" }

		val dbService =
			services.findOne(DbService::id eq ref.id.toInt()) ?: error("Le département demandé n'existe pas : $ref")
		val department = Department(
			dbService.id.toString(),
			dbService.name,
			dbService.open,
		)
		emit(Data(Result.Success(department), Data.Status.Completed, ref))
	}
}
