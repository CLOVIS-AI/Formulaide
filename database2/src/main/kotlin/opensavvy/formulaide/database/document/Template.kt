package opensavvy.formulaide.database.document

import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.cache.CacheAdapter
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.AbstractTemplateVersions
import opensavvy.formulaide.core.AbstractTemplates
import opensavvy.state.*
import opensavvy.state.slice.*
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes
import opensavvy.formulaide.core.Template as CoreTemplate

@Serializable
internal class Template(
	@SerialName("_id") val id: String,
	val name: String,
	val open: Boolean,
	val versions: List<TemplateVersion>,
)

@Serializable
internal class TemplateVersion(
	val creationDate: Instant,
	val title: String,
	val field: Field,
)

//region Version: Core -> Db

internal fun CoreTemplate.Version.toDb() = TemplateVersion(
	creationDate,
	title,
	field.toDb(),
)

//endregion

class Templates internal constructor(
	private val templates: CoroutineCollection<Template>,
	override val cache: RefCache<CoreTemplate>,
	versionCache: RefCache<CoreTemplate.Version>,
	context: CoroutineContext,
) : AbstractTemplates {
	private val templateCache = CacheAdapter<CoreTemplate.Ref, Template> {
		slice {
			val template = templates.findOne(Template::id eq it.id)
			ensureFound(template != null) { "Le modèle ${it.id} est introuvable" }
			template
		}
	}.cachedInMemory(context)
		.expireAfter(2.minutes, context)

	override suspend fun list(includeClosed: Boolean): Slice<List<CoreTemplate.Ref>> = slice {
		val filter = if (includeClosed)
			null
		else
			Template::open eq true

		val result = templates.find(filter)
			.batchSize(10)
			.toList()
			.map { CoreTemplate.Ref(it.id, this@Templates) }

		result
	}

	override suspend fun create(name: String, firstVersion: CoreTemplate.Version): Slice<CoreTemplate.Ref> = slice {
		val id = newId<Template>().toString()
		templates.insertOne(
			Template(
				id,
				name,
				open = true,
				listOf(firstVersion.copy(creationDate = Clock.System.now()).toDb())
			)
		)

		CoreTemplate.Ref(id, this@Templates)
	}

	override suspend fun createVersion(
		template: CoreTemplate.Ref,
		version: CoreTemplate.Version,
	): Slice<CoreTemplate.Version.Ref> = slice {
		val creationDate = Clock.System.now()

		templates.updateOne(
			Template::id eq template.id,
			push(Template::versions, version.copy(creationDate = creationDate).toDb())
		)

		templateCache.expire(template)
		cache.expire(template)

		CoreTemplate.Version.Ref(
			CoreTemplate.Ref(template.id, this@Templates),
			creationDate,
			versions
		)
	}

	override suspend fun edit(template: CoreTemplate.Ref, name: String?, open: Boolean?): Slice<Unit> = slice {
		val updates = ArrayList<Bson>()

		if (name != null)
			updates += setValue(Template::name, name)

		if (open != null)
			updates += setValue(Template::open, open)

		templates.updateOne(Template::id eq template.id, combine(updates))

		templateCache.expire(template)
		cache.expire(template)
	}

	override suspend fun directRequest(ref: Ref<CoreTemplate>): Slice<CoreTemplate> = slice {
		ensureValid(ref is CoreTemplate.Ref) { "${this@Templates} n'accepte pas la référence $ref" }

		val template = templateCache[ref].first().bind()
		CoreTemplate(
			template.name,
			template.versions.map { CoreTemplate.Version.Ref(ref, it.creationDate, versions) },
			template.open,
		)
	}

	inner class Versions internal constructor(
		override val cache: RefCache<CoreTemplate.Version>,
	) : AbstractTemplateVersions {
		override suspend fun directRequest(ref: Ref<CoreTemplate.Version>): Slice<CoreTemplate.Version> = slice {
			ensureValid(ref is CoreTemplate.Version.Ref) { "${this@Templates} n'accepte pas la référence $ref" }

			val template = templateCache[ref.template].first().bind()

			val version = template.versions.find { it.creationDate == ref.version }
			ensureFound(version != null) { "La version ${ref.version} du modèle ${ref.template.id} est introuvable" }

			CoreTemplate.Version(
				version.creationDate,
				version.title,
				version.field.toCore(this@Templates, this@Versions)
			)
		}
	}

	val versions = Versions(versionCache)
}
