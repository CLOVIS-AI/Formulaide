package opensavvy.formulaide.fake

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.ensureAdministrator
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureFound
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out

class FakeDepartments : Department.Service {

	private val lock = Semaphore(1)
	private val data = HashMap<String, Department>()

	override val cache: RefCache<Department> = defaultRefCache()

	private fun toRef(id: String) = Department.Ref(id, this)

	override suspend fun list(includeClosed: Boolean): Outcome<List<Department.Ref>> = out {
		if (includeClosed)
			ensureAdministrator()
		else
			ensureEmployee()

		lock.withPermit {
			data.asSequence()
				.filter { (_, it) -> it.open || includeClosed }
				.map { (id, _) -> toRef(id) }
				.toList()
		}
	}

	override suspend fun create(name: String): Outcome<Department.Ref> = out {
		ensureAdministrator()

		val id = newId()
		lock.withPermit {
			data[id] = Department(name, open = true)
		}
		toRef(id)
	}

	override suspend fun edit(department: Department.Ref, open: Boolean?): Outcome<Unit> = out {
		ensureAdministrator()

		val id = department.id
		lock.withPermit {
			if (open != null)
				data[id] = data[id]!!.copy(open = open)
		}
	}

	override suspend fun directRequest(ref: Ref<Department>): Outcome<Department> = out {
		ensureEmployee()

		ensureValid(ref is Department.Ref) { "Invalid ref $ref" }

		val result = lock.withPermit { data[ref.id] }
			?.takeIf { it.open || currentRole() >= User.Role.Administrator }
		ensureFound(result != null) { "No department with ID $ref" }

		result
	}

}
