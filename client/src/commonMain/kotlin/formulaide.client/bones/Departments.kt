package formulaide.client.bones

import formulaide.client.Client
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import io.ktor.client.request.*
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.RefCache
import opensavvy.state.Slice.Companion.successful
import opensavvy.state.State
import opensavvy.state.ensureValid
import opensavvy.state.state

class Departments(
	private val client: Client,
	override val cache: RefCache<Department>,
) : DepartmentBackbone {
	override suspend fun all(includeClosed: Boolean): List<Department.Ref> {
		val ids: List<Int> = client.get("/api/departments/list") {
			parameter("closed", includeClosed)
		}

		return ids.map { Department.Ref(it.toString(), this) }
	}

	override suspend fun create(name: String): Department.Ref {
		return client.post("/api/departments/create", name)
	}

	override suspend fun open(department: Department.Ref) {
		client.patch<String>("/api/departments/${department.id}") {
			parameter("open", true)
		}
		department.expire()
	}

	override suspend fun close(department: Department.Ref) {
		client.patch<String>("/api/departments/${department.id}") {
			parameter("open", false)
		}
		department.expire()
	}

	override fun directRequest(ref: Ref<Department>): State<Department> = state {
		ensureValid(ref is Department.Ref) { "${this@Departments} doesn't support the reference $ref" }

		val response: Department = client.get("/api/departments/${ref.id}")

		emit(successful(response))
	}
}
