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
import opensavvy.formulaide.core.Auth.Companion.currentRole
import opensavvy.formulaide.core.Auth.Companion.ensureAdministrator
import opensavvy.formulaide.core.Auth.Companion.ensureEmployee
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Form
import opensavvy.formulaide.core.Template
import opensavvy.formulaide.core.User
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
    context: CoroutineContext,
    departments: Department.Service,
    templates: Template.Service,
    private val clock: Clock,
) : Form.Service {

    private val collection = database.client.getCollection<FormDbDto>("forms")

    private val _versions = Versions(database, context, departments, templates)

    init {
        CoroutineScope(context).launch {
            _versions.collection.ensureIndex(FormVersionDbDto::form)
            _versions.collection.ensureUniqueIndex(FormVersionDbDto::form, FormVersionDbDto::version)
        }
    }

    override val cache: RefCache<Form> = defaultRefCache<Form>()
        .cachedInMemory(context)
        .expireAfter(10.minutes, context)

    override val versions: Form.Version.Service
        get() = _versions

    private fun toRef(id: String) = Form.Ref(id, this)

    override suspend fun list(includeClosed: Boolean): Outcome<List<Form.Ref>> = out {
        if (includeClosed) {
            ensureEmployee()
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
                    versions = formDto.versions.map { _versions.toRef(formDto.id, it.toInstant()) },
                )

                toRef(formDto.id)
                    .also { cache.update(it, form) }
            }
    }

    override suspend fun create(name: String, firstVersion: Form.Version): Outcome<Form.Ref> = out {
        ensureAdministrator()

        val id = newId<FormDbDto>().toString()

        firstVersion.field.validate().bind()

        collection.insertOne(
            FormDbDto(
                id = id,
                name = name,
                open = true,
                public = false,
                versions = emptySet(),
            )
        )

        toRef(id)
            .also { it.createVersion(firstVersion).bind() }
    }

    override suspend fun createVersion(form: Form.Ref, version: Form.Version): Outcome<Form.Version.Ref> = out {
        ensureAdministrator()

        // Ensure the form exists
        form.now().bind()

        version.field.validate().bind()

        // Do not trust the timestamp given by the user
        val id = clock.now()

        _versions.collection.insertOne(
            FormVersionDbDto(
                form = form.id,
                version = id.toString(),
                title = version.title,
                initial = version.field.toDto(),
                steps = version.stepsSorted.associate {
                    it.id to FormStepDbDto(
                        name = it.name,
                        reviewerDepartment = it.reviewer.id,
                        field = it.field?.toDto(),
                    )
                }
            )
        )

        collection.updateOneById(
            form.id,
            addToSet(FormDbDto::versions, id.toString()),
        )

        form.expire()

        _versions.toRef(form.id, id)
    }

    override suspend fun edit(form: Form.Ref, name: String?, open: Boolean?, public: Boolean?): Outcome<Unit> = out {
        ensureAdministrator()

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
            form.id,
            combine(updates),
        )

        form.expire()
    }

    override suspend fun directRequest(ref: Ref<Form>): Outcome<Form> = out {
        ensureValid(ref is Form.Ref) { "Référence invalide : $ref" }

        val result = collection.findOneById(ref.id)
        ensureFound(result != null) { "Formulaire introuvable : $ref" }
        ensureFound(result.public || currentRole() >= User.Role.Employee) { "Formulaire introuvable : $ref" }
        ensureFound(result.open || currentRole() >= User.Role.Employee) { "Formulaire introuvable : $ref" }

        Form(
            name = result.name,
            open = result.open,
            public = result.public,
            versions = result.versions.map { _versions.toRef(ref.id, it.toInstant()) },
        )
    }

    private inner class Versions(
        database: Database,
        context: CoroutineContext,
        private val departments: Department.Service,
        private val templates: Template.Service,
    ) : Form.Version.Service {

        val collection = database.client.getCollection<FormVersionDbDto>("formVersions")

        override val cache: RefCache<Form.Version> = defaultRefCache<Form.Version>()
            .cachedInMemory(context)
            .expireAfter(10.minutes, context)

        fun toRef(id: String, version: Instant) = Form.Version.Ref(
            toRef(id),
            version,
            this,
        )

        private suspend fun decodeTemplate(template: String, version: Instant) = Template.Version.Ref(
            Template.Ref(template, templates),
            version,
            templates.versions,
        )

        override suspend fun directRequest(ref: Ref<Form.Version>): Outcome<Form.Version> = out {
            ensureValid(ref is Form.Version.Ref) { "Référence invalide : $ref" }

            val form = ref.form.now().bind()
            if (!form.open || !form.public)
                ensureEmployee()

            val result = collection.findOne(
                and(
                    FormVersionDbDto::form eq ref.form.id,
                    FormVersionDbDto::version eq ref.version.toString(),
                )
            )
            ensureFound(result != null) { "Version du modèle introuvable : $ref" }

            Form.Version(
                creationDate = result.version.toInstant(),
                title = result.title,
                field = result.initial.toCore(::decodeTemplate),
                steps = result.steps.map { (id, it) ->
                    Form.Step(
                        id = id,
                        name = it.name,
                        reviewer = Department.Ref(it.reviewerDepartment, departments),
                        field = it.field?.toCore(::decodeTemplate),
                    )
                }
            )
        }
    }
}
