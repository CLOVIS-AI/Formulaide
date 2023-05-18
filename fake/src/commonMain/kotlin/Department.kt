package opensavvy.formulaide.fake

import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.fake.utils.newId
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.progressive.withProgress

class FakeDepartments : Department.Service<FakeDepartments.Ref> {

	private val lock = Mutex()
	private val data = HashMap<Long, Department>()

	override suspend fun list(includeClosed: Boolean): Outcome<Department.Failures.List, List<Ref>> = out {
		ensureEmployee { Department.Failures.Unauthenticated }

		if (includeClosed)
			ensureAdministrator { Department.Failures.Unauthorized }

		lock.withLock("list") {
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
		lock.withLock("create") {
			data[id] = Department(name, open = true)
		}
		Ref(id)
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text.toLong())

	inner class Ref internal constructor(
		val id: Long,
	) : Department.Ref {

		override suspend fun edit(open: Boolean?): Outcome<Department.Failures.Edit, Unit> = out {
			ensureEmployee { Department.Failures.Unauthenticated }
			ensureAdministrator { Department.Failures.Unauthorized }

			lock.withLock("edit") {
				var current = data[id]
				ensureNotNull(current) { Department.Failures.NotFound(this@Ref) }

				if (open != null)
					current = current.copy(open = open)

				data[id] = current
			}
		}

		override fun request(): ProgressiveFlow<Department.Failures.Get, Department> = flow {
			out {
				ensure(currentRole() >= User.Role.Employee) { Department.Failures.Unauthenticated }

				val result = lock.withLock("request") { data[id] }
					?.takeIf { it.open || currentRole() >= User.Role.Administrator }
				ensure(result != null) { Department.Failures.NotFound(this@Ref) }

				result
			}.also { emit(it.withProgress()) }
		}

		override fun toIdentifier() = Identifier(id.toString())

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
