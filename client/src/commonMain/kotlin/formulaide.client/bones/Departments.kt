package formulaide.client.bones

import formulaide.client.Client
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.Cache
import opensavvy.backbone.Data
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Result

class Departments(
	private val client: Client,
	override val cache: Cache<Department>,
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

	override fun directRequest(ref: opensavvy.backbone.Ref<Department>): Flow<Data<Department>> = flow {
		require(ref is Department.Ref) { "$this doesn't support the reference $ref" }

		val response: Department = client.get("/api/departments/${ref.id}")

		emit(Data(Result.Success(response), Data.Status.Completed, ref))
	}
}
