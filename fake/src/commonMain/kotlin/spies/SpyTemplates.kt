package opensavvy.formulaide.fake.spies

import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.formulaide.core.Template
import opensavvy.logger.loggerFor
import opensavvy.state.outcome.Outcome

class SpyTemplates(private val upstream: Template.Service) : Template.Service {

	private val log = loggerFor(upstream)

	override suspend fun list(includeClosed: Boolean): Outcome<List<Template.Ref>> = spy(
		log, "list", includeClosed,
	) { upstream.list(includeClosed) }

	override suspend fun create(name: String, firstVersion: Template.Version): Outcome<Template.Ref> = spy(
		log, "create", name, firstVersion,
	) { upstream.create(name, firstVersion) }

	override suspend fun createVersion(
		template: Template.Ref,
		version: Template.Version,
	): Outcome<Template.Version.Ref> =
		spy(
			log, "createVersion", template, version,
		) { upstream.createVersion(template, version) }
			.map { ref ->
				ref.copy(backbone = ref.backbone.spied())
			}

	override suspend fun edit(template: Template.Ref, name: String?, open: Boolean?): Outcome<Unit> = spy(
		log, "edit", template, name, open,
	) { upstream.edit(template, name, open) }

	override val cache: RefCache<Template>
		get() = upstream.cache

	override suspend fun directRequest(ref: Ref<Template>): Outcome<Template> = spy(
		log, "directRequest", ref,
	) { upstream.directRequest(ref) }
		.map { template ->
			template.copy(versions = template.versions.map { it.copy(backbone = it.backbone.spied()) })
		}

	class SpyVersion(private val upstream: Template.Version.Service) : Template.Version.Service {
		private val log = loggerFor(upstream)
		override val cache: RefCache<Template.Version>
			get() = upstream.cache

		override suspend fun directRequest(ref: Ref<Template.Version>): Outcome<Template.Version> = spy(
			log, "directRequest", ref,
		) { upstream.directRequest(ref) }

	}

	companion object {

		fun Template.Service.spied() = SpyTemplates(this)
		fun Template.Version.Service.spied() = SpyVersion(this)

	}
}
