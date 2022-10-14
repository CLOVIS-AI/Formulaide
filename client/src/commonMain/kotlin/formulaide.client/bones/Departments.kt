package formulaide.client.bones

import formulaide.api.rest.RestDepartment
import formulaide.api.utils.mapSuccesses
import formulaide.api.utils.onEachSuccess
import formulaide.client.Client
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import kotlinx.coroutines.flow.emitAll
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.spine.Id
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.state

class Departments(
	private val client: Client,
	override val cache: RefCache<Department>,
) : DepartmentBackbone {
	override fun all(includeClosed: Boolean): State<List<Department.Ref>> {
		val params = RestDepartment.GetParams().apply {
			this.includeClosed = includeClosed
		}

		return client.client
			.request(client.api2.departments.get, client.api2.departments.get.idOf(), Unit, params, client.context)
			.mapSuccesses { list ->
				list.map { fromId(it) }
			}
	}

	override fun create(name: String): State<Department.Ref> {
		val input = RestDepartment.New(name)

		return client.client
			.request(
				client.api2.departments.create,
				client.api2.departments.idOf(),
				input,
				Parameters.Empty,
				client.context
			)
			.mapSuccesses { (id, department) ->
				val ref = fromId(id)
				cache.update(ref to department.toCore(ref.id))
				ref
			}
	}

	override fun open(department: Department.Ref): State<Unit> {
		return client.client
			.request(
				client.api2.departments.id.open,
				client.api2.departments.id.idOf(department.id),
				Unit,
				Parameters.Empty,
				client.context
			)
			.onEachSuccess { cache.expire(department) }
	}

	override fun close(department: Department.Ref): State<Unit> {
		return client.client
			.request(
				client.api2.departments.id.close,
				client.api2.departments.id.idOf(department.id),
				Unit,
				Parameters.Empty,
				client.context
			)
			.onEachSuccess { cache.expire(department) }
	}

	override fun directRequest(ref: Ref<Department>): State<Department> = state {
		ensureValid(ref is Department.Ref) { "${this@Departments} doesn't support the reference $ref" }

		val result = client.client
			.request(
				client.api2.departments.id.get,
				client.api2.departments.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				client.context
			)
			.mapSuccesses { it.toCore(ref.id) }

		emitAll(result)
	}

	private fun fromId(id: Id) = Department.Ref(id.resource.segments[1].segment, this)
}
