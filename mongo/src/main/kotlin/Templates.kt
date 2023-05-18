package opensavvy.formulaide.mongo

import arrow.core.raise.ensureNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.now
import opensavvy.cache.cache
import opensavvy.cache.cachedInMemory
import opensavvy.cache.expireAfter
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.mongo.MongoFieldDto.Companion.toCore
import opensavvy.formulaide.mongo.MongoFieldDto.Companion.toDto
import opensavvy.state.arrow.out
import opensavvy.state.arrow.toEither
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import opensavvy.state.progressive.failed
import org.litote.kmongo.*
import kotlin.time.Duration.Companion.minutes

@Serializable
private class MongoTemplateDto(
    @SerialName("_id") val id: String,
    val name: String,
    val open: Boolean,
    val versions: Set<String>,
)

@Serializable
private class MongoTemplateVersionDto(
    val template: String,
    val version: String,
    val title: String,
    val field: MongoFieldDto,
)

class MongoTemplate(
    database: Database,
    scope: CoroutineScope,
    private val clock: Clock,
) : Template.Service {

    private val collection = database.client.getCollection<MongoTemplateDto>("templates")
    private val versionsCollection = database.client.getCollection<MongoTemplateVersionDto>("templateVersions")

    private val _versions = Versions(scope)

    init {
        scope.launch {
            versionsCollection.ensureIndex(MongoTemplateVersionDto::template)
            versionsCollection.ensureUniqueIndex(MongoTemplateVersionDto::template, MongoTemplateVersionDto::version)
        }
    }

    override val versions: Template.Version.Service
        get() = _versions

    private val cache = cache<Ref, Template.Failures.Get, Template> { ref ->
        out {
            val result = collection.findOneById(ref.id)
            ensureNotNull(result) { Template.Failures.NotFound(ref) }

            Template(
                name = result.name,
                open = result.open,
                versions = result.versions.map { _versions.Ref(ref, it.toInstant()) }
            )
        }
    }.cachedInMemory(scope.coroutineContext.job)
        .expireAfter(10.minutes, scope)

    override suspend fun list(includeClosed: Boolean): Outcome<Template.Failures.List, List<Template.Ref>> = out {
        ensureEmployee { Template.Failures.Unauthenticated }

        collection.find(
            (MongoTemplateDto::open eq true).takeIf { !includeClosed },
        )
            .toList()
            .map { templateDto ->
                val template = Template(
                    name = templateDto.name,
                    open = templateDto.open,
                    versions = templateDto.versions.map { _versions.Ref(Ref(templateDto.id), it.toInstant()) }
                )

                Ref(templateDto.id)
                    .also { cache.update(it, template) }
            }
    }

    override suspend fun create(name: String, initialVersionTitle: String, field: Field): Outcome<Template.Failures.Create, Template.Ref> = out {
        ensureEmployee { Template.Failures.Unauthenticated }
        ensureAdministrator { Template.Failures.Unauthorized }

        val id = newId<MongoTemplateDto>().toString()

        field.validate()
            .mapLeft(Template.Failures::InvalidImport)
            .bind()

        collection.insertOne(
            MongoTemplateDto(
                id = id,
                name = name,
                open = true,
                versions = emptySet(),
            )
        )

        Ref(id)
            .also { it.createVersion(initialVersionTitle, field).bind() }
    }

    override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

    inner class Ref internal constructor(
        internal val id: String,
    ) : Template.Ref {
        override suspend fun edit(name: String?, open: Boolean?): Outcome<Template.Failures.Edit, Unit> = out {
            ensureEmployee { Template.Failures.Unauthenticated }
            ensureAdministrator { Template.Failures.Unauthorized }

            val updates = buildList {
                if (name != null)
                    add(setValue(MongoTemplateDto::name, name))

                if (open != null)
                    add(setValue(MongoTemplateDto::open, open))
            }

            if (updates.isEmpty())
                return@out // nothing to do

            collection.updateOneById(
                id,
                combine(updates),
            )

            cache.expire(this@Ref)
        }

        override suspend fun createVersion(title: String, field: Field): Outcome<Template.Failures.CreateVersion, Template.Version.Ref> = out {
            ensureEmployee { Template.Failures.Unauthenticated }
            ensureAdministrator { Template.Failures.Unauthorized }

            // Ensure the template exists
            this@Ref.now().toEither()
                .mapLeft { Template.Failures.NotFound(this@Ref) }
                .bind()

            field.validate()
                .mapLeft(Template.Failures::InvalidImport)
                .bind()

            val creationDate = clock.now()

            versionsCollection.insertOne(
                MongoTemplateVersionDto(
                    template = id,
                    version = creationDate.toString(),
                    title = title,
                    field = field.toDto(),
                )
            )

            collection.updateOneById(
                id,
                addToSet(MongoTemplateDto::versions, creationDate.toString()),
            )

            cache.expire(this@Ref)

            _versions.Ref(this@Ref, creationDate)
        }

        override fun versionOf(creationDate: Instant) = _versions.Ref(this, creationDate)

        override fun request(): ProgressiveFlow<Template.Failures.Get, Template> = flow {
            if (currentRole() == User.Role.Guest) {
                emit(Template.Failures.Unauthenticated.failed())
            } else {
                emitAll(cache[this@Ref])
            }
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

        override fun toString() = "MongoTemplates.Ref($id)"
        override fun toIdentifier() = Identifier(id)

        // endregion
    }

    inner class Versions(
        scope: CoroutineScope,
    ) : Template.Version.Service {

        private val cache = cache<Ref, Template.Version.Failures.Get, Template.Version> { ref ->
            out {
                val result = versionsCollection.findOne(
                    and(
                        MongoTemplateVersionDto::template eq ref.template.id,
                        MongoTemplateVersionDto::version eq ref.creationDate.toString(),
                    )
                )
                ensureNotNull(result) { Template.Version.Failures.NotFound(ref) }

                Template.Version(
                    creationDate = result.version.toInstant(),
                    title = result.title,
                    field = result.field.toCore(this@MongoTemplate),
                )
            }
        }.cachedInMemory(scope.coroutineContext.job)
            .expireAfter(5.minutes, scope)

        inner class Ref(
            override val template: MongoTemplate.Ref,
            override val creationDate: Instant,
        ) : Template.Version.Ref {
            override fun request(): ProgressiveFlow<Template.Version.Failures.Get, Template.Version> = flow {
                if (currentRole() == User.Role.Guest) {
                    emit(Template.Version.Failures.Unauthenticated.failed())
                } else {
                    emitAll(cache[this@Ref])
                }
            }

            // region Overrides

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Ref) return false

                if (template != other.template) return false
                return creationDate == other.creationDate
            }

            override fun hashCode(): Int {
                var result = template.hashCode()
                result = 31 * result + creationDate.hashCode()
                return result
            }

            override fun toString() = "MongoTemplates.Ref(${template.id}).Version($creationDate)"
            override fun toIdentifier() = Identifier("${template.id}_$creationDate")

            // endregion
        }

        override fun fromIdentifier(identifier: Identifier): MongoTemplate.Versions.Ref {
            val (form, version) = identifier.text.split("_", limit = 2)

            return Ref(
                this@MongoTemplate.fromIdentifier(Identifier(form)),
                version.toInstant(),
            )
        }
    }
}
