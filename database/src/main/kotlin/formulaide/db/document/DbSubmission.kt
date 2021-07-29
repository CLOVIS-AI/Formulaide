package formulaide.db.document

import formulaide.api.data.FormSubmission
import formulaide.api.types.Ref
import formulaide.api.types.ReferenceId
import formulaide.db.Database
import kotlinx.serialization.Serializable
import org.litote.kmongo.eq
import org.litote.kmongo.newId

@Serializable
data class DbSubmission(
	val apiId: String,
	val form: ReferenceId,
	val root: ReferenceId? = null,
	val data: Map<String, String>,
)

suspend fun Database.saveSubmission(submission: FormSubmission): DbSubmission {
	val composites = listComposites()

	val form = findForm(submission.form.id)
		?: error("Une saisie a été reçue pour le formulaire '${submission.form}', qui n'existe pas.")

	form.load(composites)
	form.validate()
	submission.parse(form)

	return DbSubmission(
		form = form.id,
		data = submission.data,
		root = submission.root?.id,
		apiId = newId<DbSubmission>().toString()
	).also {
		submissions.insertOne(it)
	}
}

suspend fun Database.findSubmission(form: ReferenceId): List<DbSubmission> =
	submissions.find(DbSubmission::form eq form).toList()

suspend fun Database.findSubmissionById(id: ReferenceId): DbSubmission? =
	submissions.findOne(DbSubmission::apiId eq id)

fun DbSubmission.toApi() = FormSubmission(apiId, Ref(form), root?.let { Ref(it) }, data)
