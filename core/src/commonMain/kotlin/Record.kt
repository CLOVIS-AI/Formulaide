package opensavvy.formulaide.core

import kotlinx.datetime.Instant
import opensavvy.backbone.Backbone
import opensavvy.state.outcome.Outcome

/**
 * A user request and its modifications over time.
 */
data class Record(
	/**
	 * The form this record was created against.
	 */
	val form: Form.Version.Ref,
	val createdAt: Instant,
	val modifiedAt: Instant,

	/**
	 * The history of this record.
	 */
	val history: List<Diff>,
) {

	val historySorted by lazy(LazyThreadSafetyMode.PUBLICATION) { history.sortedBy { it.at } }

	init {
		require(history.isNotEmpty()) { "Un dossier ne peut pas être créé avant la saisie initiale" }

		require(history.count { it is Diff.Initial } == 1) { "Un dossier doit avoir exactement une saisie initiale : $this" }
	}

	fun currentStep(): Int? = historySorted.lastOrNull()?.step

	fun latestForStep(step: Int?): Diff? = historySorted.lastOrNull { it.step == step }

	sealed class Diff {

		abstract val author: User.Ref?

		abstract val step: Int?

		abstract val submission: Submission.Ref?

		abstract val reason: String?

		abstract val at: Instant

		data class Initial(
			override val submission: Submission.Ref,
			override val author: User.Ref?,
			override val at: Instant,
		) : Diff() {

			override val step get() = null
			override val reason get() = null
		}

		data class EditInitial(
			override val submission: Submission.Ref,
			override val author: User.Ref,
			override val reason: String,
			override val at: Instant,
		) : Diff() {
			override val step get() = null
		}

		data class Accept(
			override val submission: Submission.Ref?,
			override val author: User.Ref,
			override val step: Int,
			override val reason: String?,
			override val at: Instant,
		) : Diff()

		data class Refuse(
			override val author: User.Ref,
			override val step: Int,
			override val reason: String,
			override val at: Instant,
		) : Diff() {

			override val submission get() = null
		}

		data class MoveBack(
			override val author: User.Ref,
			override val step: Int,
			val toStep: Int,
			override val reason: String,
			override val at: Instant,
		) : Diff() {

			override val submission get() = null
		}
	}

	sealed class QueryCriterion

	data class Ref(
		val id: String,
		override val backbone: Service,
	) : opensavvy.backbone.Ref<Record>

	interface Service : Backbone<Record> {

		suspend fun search(criteria: List<QueryCriterion>): Outcome<List<Ref>>

		suspend fun search(vararg criteria: QueryCriterion) = search(criteria.asList())

		suspend fun create(submission: Submission): Outcome<Ref>

		suspend fun create(form: Form.Version.Ref, formStep: Int?, vararg data: Pair<String, String>) = create(
			Submission(
				form = form,
				formStep = formStep,
				data = data.associateBy(
					keySelector = { Field.Id.fromString(it.first) },
					valueTransform = { it.second },
				),
			)
		)

		suspend fun advance(record: Ref, diff: Diff): Outcome<Unit>

	}
}
