package formulaide.client.bones

import formulaide.client.Client
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import formulaide.core.Ref
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
	override suspend fun all(includeClosed: Boolean): List<Ref<Department>> {
		val ids: List<Int> = client.get("/api/departments/list") {
			parameter("closed", includeClosed)
		}

		return ids.map { Ref(it.toString(), this) }
	}

	override suspend fun create(name: String): Ref<Department> {
		val response: Department = client.post("/api/departments/create", name)
		val ref = Ref(response.id, this)
		cache.update(ref, response)
		return ref
	}

	override suspend fun open(department: Ref<Department>) {
		client.patch<String>("/api/departments/${department.id}") {
			parameter("open", true)
		}
		department.expire()
	}

	override suspend fun close(department: Ref<Department>) {
		client.patch<String>("/api/departments/${department.id}") {
			parameter("open", false)
		}
		department.expire()
	}

	override fun directRequest(ref: opensavvy.backbone.Ref<Department>): Flow<Data<Department>> = flow {
		require(ref is Ref) { "$this doesn't support the reference $ref" }

		val response: Department = client.get("/api/departments/${ref.id}")

		emit(Data(Result.Success(response), Data.Status.Completed, ref))
	}
}
