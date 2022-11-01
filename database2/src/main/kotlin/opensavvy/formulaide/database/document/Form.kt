package opensavvy.formulaide.database.document

import com.mongodb.client.model.Filters.and
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
import opensavvy.formulaide.core.AbstractFormVersions
import opensavvy.formulaide.core.AbstractForms
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.database.Database
import opensavvy.state.*
import opensavvy.state.slice.*
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
		slice {
			val form = forms.findOne(Form::id eq it.id)
			ensureFound(form != null) { "Le modèle ${it.id} est introuvable" }
			form
		}
	}.cachedInMemory(context)
		.expireAfter(5.minutes, context)

	override suspend fun list(
		includeClosed: Boolean,
		includePrivate: Boolean,
	): Slice<List<CoreForm.Ref>> = slice {
		val filters = ArrayList<Bson>()

		if (!includeClosed)
			filters += Form::open eq true

		if (!includePrivate)
			filters += Form::public eq true

		val filter = if (filters.isNotEmpty())
			and(filters)
		else
			null

		val result = forms.find(filter)
			.batchSize(10)
			.toList()
			.map { CoreForm.Ref(it.id, this@Forms) }

		result
	}

	override suspend fun create(
		name: String,
		public: Boolean,
		firstVersion: CoreForm.Version,
	): Slice<CoreForm.Ref> = slice {
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

		CoreForm.Ref(id, this@Forms)
	}

	override suspend fun createVersion(
		form: CoreForm.Ref,
		version: CoreForm.Version,
	): Slice<CoreForm.Version.Ref> = slice {
		val creationDate = Clock.System.now()

		forms.updateOne(
			Form::id eq form.id,
			push(Form::versions, version.copy(creationDate = creationDate).toDb())
		)

		cache.expire(form)
		formCache.expire(form)

		CoreForm.Version.Ref(form, creationDate, versions)
	}

	override suspend fun edit(
		form: CoreForm.Ref,
		name: String?,
		public: Boolean?,
		open: Boolean?,
	): Slice<Unit> = slice {
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
	}

	override suspend fun directRequest(ref: Ref<CoreForm>): Slice<CoreForm> = slice {
		ensureValid(ref is CoreForm.Ref) { "${this@Forms} n'accepte pas la référence $ref" }

		val result = formCache[ref].first().bind()

		CoreForm(
			result.name,
			result.public,
			result.open,
			result.versions.map { CoreForm.Version.Ref(ref, it.creationDate, versions) }
		)
	}

	inner class Versions internal constructor(
		override val cache: RefCache<CoreForm.Version>,
	) : AbstractFormVersions {
		override suspend fun directRequest(ref: Ref<CoreForm.Version>): Slice<CoreForm.Version> = slice {
			ensureValid(ref is CoreForm.Version.Ref) { "${this@Versions} n'accepte pas la référence $ref" }

			val forms = formCache[ref.form].first().bind()

			val version = forms.versions.find { it.creationDate == ref.version }
			ensureFound(version != null) { "La version ${ref.version} du formulaire ${ref.form.id} est introuvable" }

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
	}

	val versions = Versions(versionCache)
}
