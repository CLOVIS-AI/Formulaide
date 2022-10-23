package opensavvy.formulaide.api.client

import kotlinx.coroutines.flow.emitAll
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.formulaide.api.Department
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.core.AbstractDepartments
import opensavvy.formulaide.state.flatMapSuccess
import opensavvy.formulaide.state.flatten
import opensavvy.formulaide.state.mapSuccess
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.Slice.Companion.pending
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.state
import opensavvy.formulaide.core.Department as CoreDepartment

class Departments(
	private val client: Client,
	override val cache: RefCache<CoreDepartment>,
) : AbstractDepartments {

	override fun list(includeClosed: Boolean): State<List<CoreDepartment.Ref>> = state {
		emit(pending(0.0))
		val parameters = Department.GetParams().apply {
			this.includeClosed = includeClosed
		}

		val result = client.http
			.request(api2.departments.get, api2.departments.idOf(), Unit, parameters, client.context.value)
			.mapSuccess { list ->
				list.map { id ->
					api2.departments.id.idFrom(id, client.context.value)
						.mapSuccess { CoreDepartment.Ref(it, this@Departments) }
				}
			}
			.flatten()

		emitAll(result)
	}

	override fun create(name: String): State<CoreDepartment.Ref> = state {
		emit(pending(0.0))

		val input = Department.New(
			name,
		)

		val result = client.http
			.request(api2.departments.create, api2.departments.idOf(), input, Parameters.Empty, client.context.value)
			.mapSuccess { (id, _) -> id }
			.flatMapSuccess { emit(api2.departments.id.idFrom(it, client.context.value)) }
			.mapSuccess { CoreDepartment.Ref(it, this@Departments) }

		emitAll(result)
	}

	override fun open(department: CoreDepartment.Ref): State<Unit> = state {
		emit(pending(0.0))

		val result = client.http
			.request(
				api2.departments.id.visibility,
				api2.departments.id.idOf(department.id),
				Department.EditVisibility(open = true),
				Parameters.Empty,
				client.context.value
			)

		department.expire()

		emitAll(result)
	}

	override fun close(department: CoreDepartment.Ref): State<Unit> = state {
		emit(pending(0.0))

		val result = client.http
			.request(
				api2.departments.id.visibility,
				api2.departments.id.idOf(department.id),
				Department.EditVisibility(open = false),
				Parameters.Empty,
				client.context.value
			)

		department.expire()

		emitAll(result)
	}

	override fun directRequest(ref: Ref<CoreDepartment>): State<CoreDepartment> = state {
		emit(pending(0.0))
		ensureValid(ref is CoreDepartment.Ref) { "${this@Departments} n'accepte pas la référence $ref" }

		val result = client.http
			.request(
				api2.departments.id.get,
				api2.departments.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				client.context.value
			)
			.mapSuccess {
				CoreDepartment(
					name = it.name,
					open = it.open,
				)
			}

		emitAll(result)
	}
}
