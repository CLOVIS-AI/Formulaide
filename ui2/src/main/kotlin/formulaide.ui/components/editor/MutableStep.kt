package formulaide.ui.components.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import formulaide.ui.components.editor.MutableField.Companion.toMutable
import opensavvy.formulaide.core.Department
import opensavvy.formulaide.core.Field
import opensavvy.formulaide.core.Form

class MutableStep(
	val id: Int,
	reviewer: Department.Ref,
	field: Field?,
) {

	var reviewer by mutableStateOf(reviewer)

	var field by mutableStateOf(field?.toMutable())

}

fun Form.Step.toMutable() = MutableStep(id, reviewer, field)

fun MutableStep.toCore() = Form.Step(id, reviewer, field?.toField())
