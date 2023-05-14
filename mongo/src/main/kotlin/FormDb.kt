package opensavvy.formulaide.mongo

import arrow.core.raise.ensure
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
import opensavvy.cache.contextual.cache
import opensavvy.cache.contextual.cachedInMemory
import opensavvy.cache.contextual.expireAfter
import opensavvy.formulaide.core.*
import opensavvy.formulaide.core.utils.Identifier
import opensavvy.formulaide.mongo.FieldDbDto.Companion.toCore
import opensavvy.formulaide.mongo.FieldDbDto.Companion.toDto
import opensavvy.state.arrow.out
import opensavvy.state.arrow.toEither
import opensavvy.state.coroutines.ProgressiveFlow
import opensavvy.state.outcome.Outcome
import org.litote.kmongo.*
import kotlin.time.Duration.Companion.minutes

@Serializable
private class FormDbDto(
    @SerialName("_id") val id: String,
    val name: String,
    val open: Boolean,
    val public: Boolean,
    val versions: Set<String>,
)

@Serializable
private class FormVersionDbDto(
    val form: String,
    val version: String,
    val title: String,
    val initial: FieldDbDto,
    val steps: Map<Int, FormStepDbDto>,
)

@Serializable
private class FormStepDbDto(
    val name: String,
    val reviewerDepartment: String,
    val field: FieldDbDto?,
)

class FormDb(
    database: Database,
    scope: CoroutineScope,
    departments: Department.Service<*>,
    templates: Template.Service,
    private val clock: Clock,
) : Form.Service {

    private val collection = database.client.getCollection<FormDbDto>("forms")
    private val versionsCollection = database.client.getCollection<FormVersionDbDto>("formVersions")

    private val _versions = Versions(scope, departments, templates)

    init {
        scope.launch {
            versionsCollection.ensureIndex(FormVersionDbDto::form)
            versionsCollection.ensureUniqueIndex(FormVersionDbDto::form, FormVersionDbDto::version)
        }
    }

    private val cache = cache<Ref, User.Role, Form.Failures.Get, Form> { ref, role ->
        out {
            val form = collection.findOneById(ref.id)
            ensureNotNull(form) { Form.Failures.NotFound(ref) }

            if (role == User.Role.Guest) {
                ensure(form.public && form.open) { Form.Failures.NotFound(ref) }
            }

            Form(
                name = form.name,
                open = form.open,
                public = form.public,
                versions = form.versions.map { _versions.Ref(ref, it.toInstant()) }
            )
        }
    }.cachedInMemory(scope.coroutineContext.job)
        .expireAfter(10.minutes, scope)

    override val versions: Form.Version.Service
        get() = _versions

    override suspend fun list(includeClosed: Boolean): Outcome<Form.Failures.List, List<Form.Ref>> = out {
        if (includeClosed) {
            ensureEmployee { Form.Failures.Unauthenticated }
        }

        collection.find(
            (FormDbDto::open eq true).takeUnless { includeClosed && currentRole() >= User.Role.Employee },
            (FormDbDto::public eq true).takeUnless { currentRole() >= User.Role.Employee },
        )
            .toList()
            .map { formDto ->
                val form = Form(
                    name = formDto.name,
                    open = formDto.open,
                    public = formDto.public,
                    versions = formDto.versions.map { _versions.Ref(Ref(formDto.id), it.toInstant()) },
                )

                Ref(formDto.id)
                    .also { cache.update(it, currentRole(), form) }
            }
    }

    override suspend fun create(name: String, firstVersionTitle: String, field: Field, vararg step: Form.Step): Outcome<Form.Failures.Create, Form.Ref> = out {
        ensureEmployee { Form.Failures.Unauthenticated }
        ensureAdministrator { Form.Failures.Unauthorized }

        val id = newId<FormDbDto>().toString()

        field.validate()
            .mapLeft { Form.Failures.InvalidImport(it) }
            .bind()

        collection.insertOne(
            FormDbDto(
                id = id,
                name = name,
                open = true,
                public = false,
                versions = emptySet(),
            )
        )

        Ref(id)
            .also { it.createVersion(firstVersionTitle, field, *step).bind() }
    }

    override fun fromIdentifier(identifier: Identifier) = Ref(identifier.text)

    inner class Ref internal constructor(
        internal val id: String,
    ) : Form.Ref {
        private suspend fun edit(name: String? = null, open: Boolean? = null, public: Boolean? = null): Outcome<Form.Failures.Edit, Unit> = out {
            ensureEmployee { Form.Failures.Unauthenticated }
            ensureAdministrator { Form.Failures.Unauthorized }

            val updates = buildList {
                if (name != null)
                    add(setValue(FormDbDto::name, name))

                if (open != null)
                    add(setValue(FormDbDto::open, open))

                if (public != null)
                    add(setValue(FormDbDto::public, public))
            }

            if (updates.isEmpty())
                return@out // nothing to do

            collection.updateOneById(
                id,
                combine(updates),
            )

            cache.expire(this@Ref)
        }

        override suspend fun rename(name: String): Outcome<Form.Failures.Edit, Unit> = edit(name = name)

        override suspend fun open(): Outcome<Form.Failures.Edit, Unit> = edit(open = true)

        override suspend fun close(): Outcome<Form.Failures.Edit, Unit> = edit(open = false)

        override suspend fun publicize(): Outcome<Form.Failures.Edit, Unit> = edit(public = true)

        override suspend fun privatize(): Outcome<Form.Failures.Edit, Unit> = edit(public = false)

        override suspend fun createVersion(title: String, field: Field, vararg step: Form.Step): Outcome<Form.Failures.CreateVersion, Form.Version.Ref> = out {
            ensureEmployee { Form.Failures.Unauthenticated }
            ensureAdministrator { Form.Failures.Unauthorized }

            // Ensure the form exists
            this@Ref.now().toEither()
                .mapLeft { Form.Failures.NotFound(this@Ref) }
                .bind()

            field.validate()
                .mapLeft { Form.Failures.InvalidImport(it) }
                .bind()

            val creationDate = clock.now()

            versionsCollection.insertOne(
                FormVersionDbDto(
                    form = id,
                    version = creationDate.toString(),
                    title = title,
                    initial = field.toDto(),
                    steps = step.associate {
                        it.id to FormStepDbDto(
                            name = it.name,
                            reviewerDepartment = it.reviewer.toIdentifier().text,
                            field = it.field?.toDto(),
                        )
                    }
                )
            )

            collection.updateOneById(
                id,
                addToSet(FormDbDto::versions, creationDate.toString())
            )

            cache.expire(this@Ref)

            _versions.Ref(this@Ref, creationDate)
        }

        override fun versionOf(creationDate: Instant) = _versions.Ref(this, creationDate)

        override fun request(): ProgressiveFlow<Form.Failures.Get, Form> = flow {
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

        override fun toString() = "FormDb.Ref($id)"
        override fun toIdentifier() = Identifier(id)

        // endregion
    }

    inner class Versions internal constructor(
        scope: CoroutineScope,
        private val departments: Department.Service<*>,
        private val templates: Template.Service,
    ) : Form.Version.Service {

        private val cache = cache<Ref, User.Role, Form.Version.Failures.Get, Form.Version> { ref, role ->
            out {
                val form = ref.form.now()
                    .toEither()
                    .mapLeft { Form.Version.Failures.CouldNotGetForm(it) }
                    .bind()

                if (!form.open || !form.public) {
                    ensure(role >= User.Role.Employee) { Form.Version.Failures.NotFound(ref) }
                }

                val result = versionsCollection.findOne(
                    and(
                        FormVersionDbDto::form eq ref.form.id,
                        FormVersionDbDto::version eq ref.creationDate.toString(),
                    )
                )
                ensureNotNull(result) { Form.Version.Failures.NotFound(ref) }

                Form.Version(
                    creationDate = result.version.toInstant(),
                    title = result.title,
                    field = result.initial.toCore(templates),
                    steps = result.steps.map { (id, it) ->
                        Form.Step(
                            id = id,
                            name = it.name,
                            reviewer = departments.fromIdentifier(Identifier(it.reviewerDepartment)),
                            field = it.field?.toCore(templates),
                        )
                    }
                )
            }
        }.cachedInMemory(scope.coroutineContext.job)
            .expireAfter(10.minutes, scope)

        inner class Ref(
            override val form: FormDb.Ref,
            override val creationDate: Instant,
        ) : Form.Version.Ref {
            override fun request(): ProgressiveFlow<Form.Version.Failures.Get, Form.Version> = flow {
                emitAll(cache[this@Ref, currentRole()])
            }

            // region Overrides

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other !is Ref) return false

                if (form != other.form) return false
                return creationDate == other.creationDate
            }

            override fun hashCode(): Int {
                var result = form.hashCode()
                result = 31 * result + creationDate.hashCode()
                return result
            }

            override fun toString() = "FormsDb.Ref(${form.id}).Version($creationDate)"
            override fun toIdentifier() = Identifier("${form.id}_$creationDate")

            // endregion
        }

        override fun fromIdentifier(identifier: Identifier): Ref {
            val (form, version) = identifier.text.split("_", limit = 2)

            return Ref(
                this@FormDb.fromIdentifier(Identifier(form)),
                version.toInstant(),
            )
        }
    }
}
