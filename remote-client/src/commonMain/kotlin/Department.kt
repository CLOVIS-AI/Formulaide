package opensavvy.formulaide.remote.client

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import opensavvy.cache.contextual.cache
import opensavvy.cache.contextual.cachedInMemory
import opensavvy.cache.contextual.expireAfter
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.User
import opensavvy.formulaide.core.currentRole
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.remote.api
import opensavvy.formulaide.remote.dto.DepartmentDto
import opensavvy.spine.Parameters
import opensavvy.spine.SpineFailure
import opensavvy.spine.ktor.client.request
import opensavvy.state.arrow.out
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.map
import opensavvy.state.outcome.mapFailure
import kotlin.time.Duration.Companion.minutes

class RemoteDepartments(
	private val client: Client,
	scope: CoroutineScope,
) : Department.Service<RemoteDepartments.Ref> {

	private val cache = cache<Ref, User.Role, Department.Failures.Get, Department> { ref, role ->
		out {
			client.http.request(
				api.departments.id.get,
				api.departments.id.idOf(ref.id),
				Unit,
				Parameters.Empty,
				Unit,
			)
				.mapFailure {
					when (it.type) {
						SpineFailure.Type.Unauthenticated -> Department.Failures.Unauthenticated
						SpineFailure.Type.NotFound -> Department.Failures.NotFound(ref)
						else -> error("Received an unexpected status: $it")
					}
				}
				.map {
					Department(
						name = it.name,
						open = it.open,
					)
				}
				.bind()
		}
	}.cachedInMemory(scope.coroutineContext.job)
		.expireAfter(30.minutes, scope)

	override suspend fun list(includeClosed: Boolean): Outcome<Department.Failures.List, List<Ref>> = out {
		client.http.request(
			api.departments.get,
			api.departments.idOf(),
			Unit,
			DepartmentDto.GetParams().apply { this.includeClosed = includeClosed },
			Unit,
		)
			.mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Department.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Department.Failures.Unauthorized
					else -> error("Received an unexpected status: $it")
				}
			}
			.bind()
			.map { fromIdentifier(api.departments.id.identifierOf(it)) }
	}

	override suspend fun create(name: String): Outcome<Department.Failures.Create, Ref> = out {
		client.http.request(
			api.departments.create,
			api.departments.idOf(),
			DepartmentDto.New(name = name),
			Parameters.Empty,
			Unit
		)
			.mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Department.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Department.Failures.Unauthorized
					else -> error("Received an unexpected status: $it")
				}
			}
			.bind()
			.id
			.let { fromIdentifier(api.departments.id.identifierOf(it)) }
	}

	override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

	inner class Ref internal constructor(
		internal val id: String,
	) : Department.Ref {
		private suspend fun edit(open: Boolean? = null) = out {
			client.http.request(
				api.departments.id.edit,
				api.departments.id.idOf(id),
				DepartmentDto.Edit(open = open),
				Parameters.Empty,
				Unit,
			).mapFailure {
				when (it.type) {
					SpineFailure.Type.Unauthenticated -> Department.Failures.Unauthenticated
					SpineFailure.Type.Unauthorized -> Department.Failures.Unauthorized
					SpineFailure.Type.NotFound -> Department.Failures.NotFound(this@Ref)
					else -> error("Received an unexpected status: $it")
				}
			}.bind()

			cache.expire(this@Ref)
		}

		override suspend fun open(): Outcome<Department.Failures.Edit, Unit> = edit(open = true)

		override suspend fun close(): Outcome<Department.Failures.Edit, Unit> = edit(open = false)

		override fun request(): ProgressiveFlow<Department.Failures.Get, Department> = flow {
			emitAll(cache[this@Ref, currentRole()])
		}

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is Ref) return false

			return id == other.id
		}

		override fun hashCode(): Int {
			return id.hashCode()
		}

		override fun toString() = "RemoteDepartments.Ref($id)"

		override fun toIdentifier() = Identifier(id)

		// endregion
	}
}
