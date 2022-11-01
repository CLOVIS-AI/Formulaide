package formulaide.client.bones

import formulaide.api.rest.RestDepartment
import formulaide.client.Client
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.spine.Id
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.slice.*

class Departments(
	private val client: Client,
	override val cache: RefCache<Department>,
) : DepartmentBackbone {
	override fun all(includeClosed: Boolean): Flow<Slice<List<Department.Ref>>> = flow {
		val params = RestDepartment.GetParams().apply {
			this.includeClosed = includeClosed
		}

		val list = client.client
			.request(client.api2.departments.get, client.api2.departments.get.idOf(), Unit, params, client.context)
			.valueOrThrow

		emit(successful(list.map { fromId(it) }))
	}

	override fun create(name: String): Flow<Slice<Department.Ref>> = flow {
		val input = RestDepartment.New(name)

		val (id, department) = client.client
			.request(
				client.api2.departments.create,
				client.api2.departments.idOf(),
				input,
				Parameters.Empty,
				client.context
			).valueOrThrow

		val ref = fromId(id)
		cache.update(ref to department.toCore(ref.id))
		emit(successful(ref))
	}

	override fun open(department: Department.Ref): Flow<Slice<Unit>> = flow {
		client.client
			.request(
				client.api2.departments.id.open,
				client.api2.departments.id.idOf(department.id),
				Unit,
				Parameters.Empty,
				client.context
			).valueOrThrow
		cache.expire(department)
		emit(successful(Unit))
	}

	override fun close(department: Department.Ref): Flow<Slice<Unit>> = flow {
		client.client
			.request(
				client.api2.departments.id.close,
				client.api2.departments.id.idOf(department.id),
				Unit,
				Parameters.Empty,
				client.context
			).valueOrThrow
		cache.expire(department)
		emit(successful(Unit))
	}

	override suspend fun directRequest(ref: Ref<Department>): Slice<Department> = slice {
		ensureValid(ref is Department.Ref) { "${this@Departments} doesn't support the reference $ref" }

		val result = client.client
			.request(
				client.api2.departments.id.get,
				client.api2.departments.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				client.context
			).bind()

		result.toCore(ref.id)
	}

	private fun fromId(id: Id) = Department.Ref(id.resource.segments[1].segment, this)
}
