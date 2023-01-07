package opensavvy.formulaide.remote.client

import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.DepartmentDto
import opensavvy.formulaide.remote.dto.DepartmentDto.Companion.toCore
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

class Departments(
	private val client: Client,
	cacheContext: CoroutineContext,
) : Department.Service {

	override val cache: RefCache<Department> = defaultRefCache<Department>()
		.cachedInMemory(cacheContext)
		.expireAfter(30.minutes, cacheContext)

	override suspend fun list(includeClosed: Boolean): Outcome<List<Department.Ref>> = out {
		client.http.request(
			api.departments.get,
			api.departments.idOf(),
			Unit,
			DepartmentDto.GetParams().apply { this.includeClosed = includeClosed },
			Unit,
		).bind()
			.map { api.departments.id.refOf(it, this@Departments).bind() }
	}

	override suspend fun create(name: String): Outcome<Department.Ref> = out {
		client.http.request(
			api.departments.create,
			api.departments.idOf(),
			DepartmentDto.New(name = name),
			Parameters.Empty,
			Unit
		)
			.bind()
			.id
			.let { api.departments.id.refOf(it, this@Departments) }
			.bind()
	}

	override suspend fun edit(department: Department.Ref, open: Boolean?): Outcome<Unit> = out {
		client.http.request(
			api.departments.id.edit,
			api.departments.id.idOf(department.id),
			DepartmentDto.Edit(open = open),
			Parameters.Empty,
			Unit,
		).bind()

		department.expire()
	}

	override suspend fun directRequest(ref: Ref<Department>): Outcome<Department> = out {
		ensureValid(ref is Department.Ref) { "Expected Department.Ref, found $ref" }

		client.http.request(
			api.departments.id.get,
			api.departments.id.idOf(ref.id),
			Unit,
			Parameters.Empty,
			Unit,
		).bind()
			.toCore()
	}

}
