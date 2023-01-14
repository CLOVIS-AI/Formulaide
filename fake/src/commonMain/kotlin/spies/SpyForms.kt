package opensavvy.formulaide.fake.spies

import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.formulaide.core.Form
import opensavvy.logger.loggerFor
import opensavvy.state.outcome.Outcome

class SpyForms(private val upstream: Form.Service) : Form.Service {

	private val log = loggerFor(upstream)
	override suspend fun list(includeClosed: Boolean): Outcome<List<Form.Ref>> = spy(
		log, "list", includeClosed,
	) { upstream.list(includeClosed) }
		.map { it.map { it.copy(backbone = this) } }

	override suspend fun create(name: String, firstVersion: Form.Version): Outcome<Form.Ref> = spy(
		log, "create", name, firstVersion,
	) { upstream.create(name, firstVersion) }
		.map { it.copy(backbone = this) }

	override suspend fun createVersion(form: Form.Ref, version: Form.Version): Outcome<Form.Version.Ref> = spy(
		log, "createVersion", form, version,
	) { upstream.createVersion(form, version) }
		.map { it.copy(backbone = it.backbone.spied()) }

	override suspend fun edit(form: Form.Ref, name: String?, open: Boolean?, public: Boolean?): Outcome<Unit> = spy(
		log, "edit", form, name, open, public,
	) { upstream.edit(form, name, open, public) }

	override val cache: RefCache<Form>
		get() = upstream.cache

	override suspend fun directRequest(ref: Ref<Form>): Outcome<Form> = spy(
		log, "directRequest", ref,
	) { upstream.directRequest(ref) }
		.map { form -> form.copy(versions = form.versions.map { it.copy(backbone = it.backbone.spied()) }) }

	class SpyVersion(private val upstream: Form.Version.Service) : Form.Version.Service {
		private val log = loggerFor(upstream)

		override val cache: RefCache<Form.Version>
			get() = upstream.cache

		override suspend fun directRequest(ref: Ref<Form.Version>): Outcome<Form.Version> = spy(
			log, "directRequest", ref,
		) { upstream.directRequest(ref) }

	}

	companion object {
		fun Form.Service.spied() = SpyForms(this)
		fun Form.Version.Service.spied() = SpyVersion(this)
	}

}
