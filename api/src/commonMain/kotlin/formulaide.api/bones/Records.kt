package formulaide.api.bones

import formulaide.core.form.Form
import formulaide.core.form.Submission
import formulaide.core.record.Record
import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
class ApiNewRecord(
	val form: @Contextual Form.Ref,
	val version: Instant,
	val submission: Submission,
)

@Serializable
class ApiRecordReview(
	val step: Int?,
	val decision: Record.Decision,
	val reason: String?,
	val submission: Submission?,
)
