package formulaide.db.document

import formulaide.api.data.FormSubmission
import formulaide.api.types.Ref
import formulaide.api.types.ReferenceId
import formulaide.db.Database
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.litote.kmongo.Id
import org.litote.kmongo.eq
import org.litote.kmongo.newId

@Serializable
data class DbSubmission(
	@SerialName("_id") @Contextual val id: Id<DbSubmission> = newId(),
	val form: ReferenceId,
	val action: ReferenceId? = null,
	val data: Map<String, String>,
)

suspend fun Database.saveSubmission(submission: FormSubmission): DbSubmission {
	val composites = listComposites()

	val form = findForm(submission.form.id)
		?: error("Une saisie a été reçue pour le formulaire '${submission.form}', qui n'existe pas.")

	form.load(composites)
	form.validate()
	submission.checkValidity(form)

	return DbSubmission(form = form.id, data = submission.data).also {
		submissions.insertOne(it)
	}
}

suspend fun Database.findSubmission(form: ReferenceId): List<DbSubmission> =
	submissions.find(DbSubmission::form eq form).toList()

fun DbSubmission.toApi() = FormSubmission(id.toString(), Ref(form), action?.let { Ref(it) }, data)
