package opensavvy.formulaide.api.client

import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.formulaide.api.Department
import opensavvy.formulaide.api.api2
import opensavvy.formulaide.core.AbstractDepartments
import opensavvy.spine.Parameters
import opensavvy.spine.ktor.client.request
import opensavvy.state.Progression.Companion.loading
import opensavvy.state.ProgressionReporter.Companion.report
import opensavvy.state.slice.Slice
import opensavvy.state.slice.ensureValid
import opensavvy.state.slice.slice
import opensavvy.formulaide.core.Department as CoreDepartment

class Departments(
	private val client: Client,
	override val cache: RefCache<CoreDepartment>,
) : AbstractDepartments {

	override suspend fun list(includeClosed: Boolean): Slice<List<CoreDepartment.Ref>> = slice {
		report(loading(0.0))
		val parameters = Department.GetParams().apply {
			this.includeClosed = includeClosed
		}

		val list = client.http
			.request(api2.departments.get, api2.departments.idOf(), Unit, parameters, client.context.value)
			.bind()

		list.map { id ->
			CoreDepartment.Ref(api2.departments.id.idFrom(id, client.context.value).bind(), this@Departments)
		}
	}

	override suspend fun create(name: String): Slice<CoreDepartment.Ref> = slice {
		report(loading(0.0))

		val input = Department.New(
			name,
		)

		val (id, _) = client.http
			.request(api2.departments.create, api2.departments.idOf(), input, Parameters.Empty, client.context.value)
			.bind()

		CoreDepartment.Ref(api2.departments.id.idFrom(id, client.context.value).bind(), this@Departments)
	}

	override suspend fun open(department: CoreDepartment.Ref): Slice<Unit> = slice {
		report(loading(0.0))

		client.http
			.request(
				api2.departments.id.visibility,
				api2.departments.id.idOf(department.id),
				Department.EditVisibility(open = true),
				Parameters.Empty,
				client.context.value
			).bind()

		department.expire()
	}

	override suspend fun close(department: CoreDepartment.Ref): Slice<Unit> = slice {
		report(loading(0.0))

		client.http
			.request(
				api2.departments.id.visibility,
				api2.departments.id.idOf(department.id),
				Department.EditVisibility(open = false),
				Parameters.Empty,
				client.context.value
			)

		department.expire()
	}

	override suspend fun directRequest(ref: Ref<CoreDepartment>): Slice<CoreDepartment> = slice {
		report(loading(0.0))
		ensureValid(ref is CoreDepartment.Ref) { "${this@Departments} n'accepte pas la référence $ref" }

		val result = client.http
			.request(
				api2.departments.id.get,
				api2.departments.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				client.context.value
			).bind()

		CoreDepartment(
			name = result.name,
			open = result.open,
		)
	}
}
