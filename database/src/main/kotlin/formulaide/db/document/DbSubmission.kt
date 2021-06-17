package formulaide.db.document

import formulaide.api.data.FormId
import formulaide.api.data.FormSubmission
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
	val form: FormId,
	val data: Map<String, String>,
)

suspend fun Database.saveSubmission(submission: FormSubmission): DbSubmission {
	val compounds = listData()
	val form = findForm(submission.form)
		?: error("Une saisie a été reçue pour le formulaire '${submission.form}', qui n'existe pas.")

	submission.checkValidity(form, compounds)

	return DbSubmission(form = submission.form, data = submission.data).also {
		submissions.insertOne(it)
	}
}

suspend fun Database.findSubmission(form: FormId): List<DbSubmission> =
	submissions.find(DbSubmission::form eq form).toList()
