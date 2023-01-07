package opensavvy.formulaide.fake.spies

import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.formulaide.core.Department
import opensavvy.logger.loggerFor
import opensavvy.state.slice.Slice

class SpyDepartments(private val upstream: Department.Service) : Department.Service {

	private val log = loggerFor(upstream)

	override suspend fun list(includeClosed: Boolean): Slice<List<Department.Ref>> = spy(
		log, "list", includeClosed
	) { upstream.list(includeClosed) }
		.map { it.map { it.copy(backbone = this) } }

	override suspend fun create(name: String): Slice<Department.Ref> = spy(
		log, "create", name,
	) { upstream.create(name) }
		.map { it.copy(backbone = this) }

	override suspend fun edit(department: Department.Ref, open: Boolean?): Slice<Unit> = spy(
		log, "edit", department, open,
	) { upstream.edit(department, open) }

	override val cache: RefCache<Department>
		get() = upstream.cache

	override suspend fun directRequest(ref: Ref<Department>): Slice<Department> = spy(
		log, "directRequest", ref,
	) { upstream.directRequest(ref) }

	companion object {

		fun Department.Service.spied() = SpyDepartments(this)
	}
}
