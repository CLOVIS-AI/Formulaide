package opensavvy.formulaide.mongo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.Ref.Companion.expire
import opensavvy.backbone.Ref.Companion.now
import opensavvy.backbone.RefCache
import opensavvy.backbone.defaultRefCache
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.Auth.Companion.ensureAdministrator
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.mongo.FieldDbDto.Companion.toCore
import opensavvy.formulaide.mongo.FieldDbDto.Companion.toDto
import opensavvy.state.outcome.Outcome
import opensavvy.state.outcome.ensureFound
import opensavvy.state.outcome.ensureValid
import opensavvy.state.outcome.out
import org.litote.kmongo.*
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes

@Serializable
private class TemplateDbDto(
    @SerialName("_id") val id: String,
    val name: String,
    val open: Boolean,
    val versions: Set<String>,
)

@Serializable
private class TemplateVersionDbDto(
    val template: String,
    val version: String,
    val title: String,
    val field: FieldDbDto,
)

class TemplateDb(
    database: Database,
    context: CoroutineContext,
    private val clock: Clock,
) : Template.Service {

    private val collection = database.client.getCollection<TemplateDbDto>("templates")

    private val _versions = Versions(database, context)

    init {
        CoroutineScope(context).launch {
            _versions.collection.ensureIndex(TemplateVersionDbDto::template)
            _versions.collection.ensureUniqueIndex(TemplateVersionDbDto::template, TemplateVersionDbDto::version)
        }
    }

    override val versions: Template.Version.Service
        get() = _versions

    override val cache: RefCache<Template> = defaultRefCache<Template>()
        .cachedInMemory(context)
        .expireAfter(10.minutes, context)

    private fun toRef(id: String) = Template.Ref(id, this)

    override suspend fun list(includeClosed: Boolean): Outcome<List<Template.Ref>> = out {
        ensureEmployee()

        collection.find(
            (TemplateDbDto::open eq true).takeIf { !includeClosed },
        )
            .toList()
            .map { templateDto ->
                val template = Template(
                    name = templateDto.name,
                    open = templateDto.open,
                    versions = templateDto.versions.map { _versions.toRef(templateDto.id, it.toInstant()) }
                )

                toRef(templateDto.id)
                    .also { cache.update(it, template) }
            }
    }

    override suspend fun create(name: String, firstVersion: Template.Version): Outcome<Template.Ref> = out {
        ensureAdministrator()

        val id = newId<TemplateDbDto>().toString()

        collection.insertOne(
            TemplateDbDto(
                id = id,
                name = name,
                open = true,
                versions = emptySet(),
            )
        )

        toRef(id)
            .also { it.createVersion(firstVersion).bind() }
    }

    override suspend fun createVersion(
        template: Template.Ref,
        version: Template.Version
    ): Outcome<Template.Version.Ref> = out {
        ensureAdministrator()

        // Ensure the template exists
        template.now().bind()

        version.field.validate().bind()

        // Do not trust the timestamp given by the user
        val id = clock.now()

        _versions.collection.insertOne(
            TemplateVersionDbDto(
                template = template.id,
                version = id.toString(),
                title = version.title,
                field = version.field.toDto(),
            )
        )

        collection.updateOneById(
            template.id,
            addToSet(TemplateDbDto::versions, id.toString()),
        )

        template.expire()

        _versions.toRef(template.id, id)
    }

    override suspend fun edit(template: Template.Ref, name: String?, open: Boolean?): Outcome<Unit> = out {
        ensureAdministrator()

        val updates = buildList {
            if (name != null)
                add(setValue(TemplateDbDto::name, name))

            if (open != null)
                add(setValue(TemplateDbDto::open, open))
        }

        if (updates.isEmpty())
            return@out // nothing to do

        collection.updateOneById(
            template.id,
            combine(updates),
        )

        template.expire()
    }

    override suspend fun directRequest(ref: Ref<Template>): Outcome<Template> = out {
        ensureValid(ref is Template.Ref) { "Référence invalide : $ref" }
        ensureEmployee()

        val result = collection.findOneById(ref.id)
        ensureFound(result != null) { "Modèle introuvable : $ref" }

        Template(
            name = result.name,
            open = result.open,
            versions = result.versions.map { _versions.toRef(ref.id, it.toInstant()) },
        )
    }

    private inner class Versions(
        database: Database,
        context: CoroutineContext,
    ) : Template.Version.Service {

        val collection = database.client.getCollection<TemplateVersionDbDto>("templateVersions")

        override val cache: RefCache<Template.Version> = defaultRefCache<Template.Version>()
            .cachedInMemory(context)
            .expireAfter(5.minutes, context)

        fun toRef(id: String, version: Instant) = Template.Version.Ref(
            toRef(id),
            version,
            this,
        )

        override suspend fun directRequest(ref: Ref<Template.Version>): Outcome<Template.Version> = out {
            ensureValid(ref is Template.Version.Ref) { "Référence invalide : $ref" }
            ensureEmployee()

            val result = collection.findOne(
                and(
                    TemplateVersionDbDto::template eq ref.template.id,
                    TemplateVersionDbDto::version eq ref.version.toString(),
                )
            )
            ensureFound(result != null) { "Version du modèle introuvable : $ref" }

            Template.Version(
                creationDate = result.version.toInstant(),
                title = result.title,
                field = result.field.toCore(decodeTemplate = { template, version -> toRef(template, version) }),
            )
        }

    }
}
