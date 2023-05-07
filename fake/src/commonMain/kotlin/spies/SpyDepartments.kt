package opensavvy.formulaide.fake.spies

import opensavvy.backbone.Ref
import opensavvy.formulaide.core.Department
import opensavvy.logger.loggerFor
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.map

class SpyDepartments<D : Department.Ref>(private val upstream: Department.Service<D>) : Department.Service<SpyDepartments<D>.Ref> {

	private val log = loggerFor(upstream)

	override suspend fun list(includeClosed: Boolean): Outcome<Department.Failures.List, List<Ref>> = spy(
		log, "list", includeClosed
	) { upstream.list(includeClosed) }
		.map { it.map(::Ref) }

	override suspend fun create(name: String) = spy(
		log, "create", name,
	) { upstream.create(name) }
		.map(::Ref)

	inner class Ref internal constructor(
		private val upstream: D,
	) : Department.Ref {
		override suspend fun open(): Outcome<Department.Failures.Edit, Unit> = spy(
			log, "open",
		) { upstream.open() }

		override suspend fun close(): Outcome<Department.Failures.Edit, Unit> = spy(
			log, "close",
		) { upstream.close() }

		override fun request(): ProgressiveFlow<Department.Failures.Get, Department> = spy(
			log, "request",
		) { upstream.request() }

		// region Overrides

		override fun equals(other: Any?): Boolean {
			if (this === other) return true
			if (other !is SpyDepartments<*>.Ref) return false

			return upstream == other.upstream
		}

		override fun hashCode(): Int {
			return upstream.hashCode()
		}

		override fun toString() = upstream.toString()

		// endregion
	}

	companion object {

		fun <D : Department.Ref> Department.Service<D>.spied() = SpyDepartments(this)
	}
}
