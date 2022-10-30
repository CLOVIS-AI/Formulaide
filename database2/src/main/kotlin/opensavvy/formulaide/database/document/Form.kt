package opensavvy.formulaide.database.document

import com.mongodb.client.model.Filters.and
import kotlinx.coroutines.flow.emitAll
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import opensavvy.backbone.Ref
import opensavvy.backbone.RefCache
import opensavvy.cache.CacheAdapter
import opensavvy.cache.ExpirationCache.Companion.expireAfter
import opensavvy.cache.MemoryCache.Companion.cachedInMemory
import opensavvy.formulaide.core.AbstractFormVersions
import opensavvy.formulaide.core.AbstractForms
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.database.Database
import opensavvy.state.*
import opensavvy.state.Slice.Companion.successful
import org.bson.conversions.Bson
import org.litote.kmongo.*
import org.litote.kmongo.coroutine.CoroutineCollection
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.minutes
import opensavvy.formulaide.core.Form as CoreForm

@Serializable
internal class Form(
	@SerialName("_id") val id: String,
	val name: String,
	val open: Boolean,
	val public: Boolean,
	val versions: List<FormVersion>,
)

@Serializable
internal class FormVersion(
	val creationDate: Instant,
	val title: String,
	val field: Field,
	val steps: List<ReviewStep>,
)

@Serializable
internal class ReviewStep(
	val id: Int,
	val department: String,
	val field: Field?,
)

//region Version: Core -> Db

internal fun CoreForm.Version.toDb() = FormVersion(
	creationDate,
	title,
	field.toDb(),
	reviewSteps.map { ReviewStep(it.id, it.reviewer.id, it.field?.toDb()) }
)

//endregion

class Forms internal constructor(
	private val forms: CoroutineCollection<Form>,
	override val cache: RefCache<CoreForm>,
	versionCache: RefCache<CoreForm.Version>,
	context: CoroutineContext,
	private val database: Database,
) : AbstractForms {
	private val formCache = CacheAdapter<CoreForm.Ref, Form> {
		state {
			val form = forms.findOne(Form::id eq it.id)
			ensureFound(form != null) { "Le modèle ${it.id} est introuvable" }
			emit(successful(form))
		}
	}.cachedInMemory(context)
		.expireAfter(5.minutes, context)

	override fun list(
		includeClosed: Boolean,
		includePrivate: Boolean,
	): State<List<CoreForm.Ref>> = state {
		val filters = ArrayList<Bson>()

		if (!includeClosed)
			filters += Form::open eq true

		if (!includePrivate)
			filters += Form::public eq true

		val result = forms.find(and(filters))
			.batchSize(10)
			.toList()
			.map { CoreForm.Ref(it.id, this@Forms) }

		emit(successful(result))
	}

	override fun create(
		name: String,
		public: Boolean,
		firstVersion: CoreForm.Version,
	): State<CoreForm.Ref> = state {
		val id = newId<Form>().toString()
		forms.insertOne(
			Form(
				id,
				name,
				open = true,
				public = public,
				listOf(firstVersion.copy(creationDate = Clock.System.now()).toDb())
			)
		)

		emit(successful(CoreForm.Ref(id, this@Forms)))
	}

	override fun createVersion(
		form: CoreForm.Ref,
		new: CoreForm.Version,
	): State<Unit> = state {
		forms.updateOne(
			Form::id eq form.id,
			push(Form::versions, new.copy(creationDate = Clock.System.now()).toDb())
		)

		cache.expire(form)
		formCache.expire(form)

		emit(successful(Unit))
	}

	override fun edit(
		form: CoreForm.Ref,
		name: String?,
		public: Boolean?,
		open: Boolean?,
	): State<Unit> = state {
		val updates = ArrayList<Bson>()

		if (name != null)
			updates += setValue(Form::name, name)

		if (public != null)
			updates += setValue(Form::public, public)

		if (open != null)
			updates += setValue(Form::open, open)

		forms.updateOne(
			Form::id eq form.id,
			combine(updates),
		)

		cache.expire(form)
		formCache.expire(form)

		emit(successful(Unit))
	}

	override fun directRequest(ref: Ref<CoreForm>): State<CoreForm> = state {
		ensureValid(ref is CoreForm.Ref) { "${this@Forms} n'accepte pas la référence $ref" }

		val result = formCache[ref]
			.mapSuccess { form ->
				CoreForm(
					form.name,
					form.public,
					form.open,
					form.versions.map { CoreForm.Version.Ref(ref, it.creationDate, versions) }
				)
			}

		emitAll(result)
	}

	inner class Versions internal constructor(
		override val cache: RefCache<CoreForm.Version>,
	) : AbstractFormVersions {
		override fun directRequest(ref: Ref<CoreForm.Version>): State<CoreForm.Version> = state {
			ensureValid(ref is CoreForm.Version.Ref) { "${this@Versions} n'accepte pas la référence $ref" }

			val result = formCache[ref.form]
				.flatMapSuccess { form ->
					val version = form.versions.find { it.creationDate == ref.version }
					ensureFound(version != null) { "La version ${ref.version} du formulaire ${ref.form.id} est introuvable" }
					emit(successful(version))
				}
				.mapSuccess { version ->
					CoreForm.Version(
						version.creationDate,
						version.title,
						version.field.toCore(database.templates, database.templates.versions),
						version.steps.map { step ->
							CoreForm.Step(
								step.id,
								Department.Ref(step.department, database.departments),
								step.field?.toCore(database.templates, database.templates.versions),
							)
						}
					)
				}

			emitAll(result)
		}
	}

	private val versions = Versions(versionCache)
}
