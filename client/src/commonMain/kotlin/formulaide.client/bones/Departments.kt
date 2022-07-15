package formulaide.client.bones

import formulaide.api.bones.ApiDepartment
import formulaide.api.bones.ApiDepartment.Companion.toCore
import formulaide.client.Client
import formulaide.core.Department
import formulaide.core.DepartmentBackbone
import io.ktor.client.request.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import opensavvy.backbone.*
import opensavvy.backbone.Ref.Companion.expire

data class DepartmentRef(
	val id: Int,
	override val backbone: Backbone<Department>,
) : Ref<Department>

class Departments(
	private val client: Client,
	override val cache: Cache<Department>,
) : DepartmentBackbone {
	override suspend fun all(includeClosed: Boolean): List<DepartmentRef> {
		val ids: List<Int> = client.get("/api/departments/list") {
			parameter("closed", includeClosed)
		}

		return ids.map { DepartmentRef(it, this) }
	}

	override suspend fun create(name: String): DepartmentRef {
		val response: ApiDepartment = client.post("/api/departments/create", name)
		val ref = DepartmentRef(response.id, this)
		cache.update(ref, response.toCore())
		return ref
	}

	override suspend fun open(department: Ref<Department>) {
		require(department is DepartmentRef) { "$this doesn't support the reference $department" }

		client.patch<String>("/api/departments/${department.id}") {
			parameter("open", true)
		}
		department.expire()
	}

	override suspend fun close(department: Ref<Department>) {
		require(department is DepartmentRef) { "$this doesn't support the reference $department" }

		client.patch<String>("/api/departments/${department.id}") {
			parameter("open", false)
		}
		department.expire()
	}

	override fun directRequest(ref: Ref<Department>): Flow<Data<Department>> = flow {
		require(ref is DepartmentRef) { "$this doesn't support the reference $ref" }

		val response: ApiDepartment = client.get("/api/departments/${ref.id}")

		emit(Data(Result.Success(response.toCore()), Data.Status.Completed, ref))
	}
}
