package opensavvy.formulaide.fake

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import opensavvy.formulaide.core.*
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.progressive.withProgress

class FakeDepartments : Department.Service<FakeDepartments.Ref> {

	private val lock = Semaphore(1)
	private val data = HashMap<Long, Department>()

	override suspend fun list(includeClosed: Boolean): Outcome<Department.Failures.List, List<Ref>> = out {
		ensureEmployee { Department.Failures.Unauthenticated }

		if (includeClosed)
			ensureAdministrator { Department.Failures.Unauthorized }

		lock.withPermit {
			data.asSequence()
				.filter { (_, it) -> it.open || includeClosed }
				.map { (id, _) -> Ref(id) }
				.toList()
		}
	}

	override suspend fun create(name: String): Outcome<Department.Failures.Create, Ref> = out {
		ensureEmployee { Department.Failures.Unauthenticated }
		ensureAdministrator { Department.Failures.Unauthorized }

		val id = newId()
		lock.withPermit {
			data[id] = Department(name, open = true)
		}
		Ref(id)
	}

	inner class Ref internal constructor(
		val id: Long,
	) : Department.Ref {

		private suspend fun Raise<Department.Failures.Edit>.edit(open: Boolean?) {
			ensureEmployee { Department.Failures.Unauthenticated }
			ensureAdministrator { Department.Failures.Unauthorized }

			lock.withPermit {
				var current = data[id]
				ensureNotNull(current) { Department.Failures.NotFound(this@Ref) }

				if (open != null)
					current = current.copy(open = open)

				data[id] = current
			}
		}

		override suspend fun open(): Outcome<Department.Failures.Edit, Unit> = out {
			edit(open = true)
		}

		override suspend fun close(): Outcome<Department.Failures.Edit, Unit> = out {
			edit(open = false)
		}

		override fun request(): ProgressiveFlow<Department.Failures.Get, Department> = flow {
			out {
				ensure(currentRole() >= User.Role.Employee) { Department.Failures.Unauthenticated }

				val result = lock.withPermit { data[id] }
					?.takeIf { it.open || currentRole() >= User.Role.Administrator }
				ensure(result != null) { Department.Failures.NotFound(this@Ref) }

				result
			}.also { emit(it.withProgress()) }
		}

		// region equals & hashCode

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Ref) return false

			return id == other.id
		}

		override fun hashCode(): Int {
			return id.hashCode()
		}

		// endregion
	}
}
