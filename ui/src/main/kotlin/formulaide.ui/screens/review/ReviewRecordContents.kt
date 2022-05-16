package formulaide.ui.screens.review

import formulaide.api.data.RecordState
import formulaide.api.types.Ref
import formulaide.ui.components.LoadingSpinner
import formulaide.ui.components.inputs.Nesting
import formulaide.ui.components.text.Title
import formulaide.ui.fields.editors.ImmutableField
import formulaide.ui.fields.renderers.Field
import formulaide.ui.reportExceptions
import formulaide.ui.utils.classes
import react.FC
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.useEffect
import react.useState
import kotlin.js.Date

internal external interface ReviewRecordContentsProps : ReviewRecordVariantProps {
	var selectedDestination: RecordState?
}

/**
 * Draws the contents of a [ReviewRecord], with the user's submission.
 */
internal val ReviewRecordContents = FC<ReviewRecordContentsProps>("ReviewRecordContents") { props ->
	var i = 0

	var loaded by useState(false)
	useEffect(props.referencedComposites) {
		val state = props.windowState
		if (state is RecordState.Action) reportExceptions {
			state.current.loadFrom(props.form.actions, lazy = true)
			val action = state.current.obj

			loaded = try {
				action.fields?.load(props.referencedComposites, allowNotFound = false)
				true
			} catch (e: Ref.MissingElementToLoadReferenceException) {
				console.log("ReviewRecordContents: not all composites are loaded: $e")
				false
			}
		} else {
			loaded = true
		}
	}

	if (!loaded) {
		+"Chargement des champs…"
		LoadingSpinner()
		return@FC
	}

	// What the user has filled in, and the history of this record
	for (parsed in props.history) {
		Nesting {
			depth = 0
			fieldNumber = i

			val transition = parsed.transition
			val title = transition.previousState.displayName()
			if (props.showFullHistory == true) {
				Title { this.title = "$title → ${transition.nextState.displayName()}" }
			} else {
				Title { this.title = title }
			}

			val timestamp = Date(transition.timestamp * 1000)
			if (transition.previousState != null) {
				p {
					+"Par ${transition.assignee?.id}"
					if (transition.reason != null)
						+" parce que \"${transition.reason}\""
					+", le ${timestamp.toLocaleString()}."
				}
			} else {
				p { +"Le ${timestamp.toLocaleString()}." }
			}

			if (transition.fields != null) {
				if (parsed.submission == null) {
					p { +"Chargement de la saisie…"; LoadingSpinner() }
				} else {
					br()
					for (answer in parsed.submission.fields) {
						ImmutableField {
							this.answer = answer
						}
					}
				}
			}
		}

		i++
	}

	val state = props.windowState
	if (state is RecordState.Action && props.windowState != null && props.selectedDestination !is RecordState.Refused) div {
		classes = "print:hidden"

		val action = state.current.obj

		val root = action.fields
		if (root != null) {
			Nesting {
				depth = 0
				fieldNumber = i

				for (field in root.fields) {
					Field {
						this.form = props.form
						this.root = action
						this.field = field
						this.fieldKey = "${props.record.id}_${field.id}"
					}
				}
				i++
			}
		}
	}
}
