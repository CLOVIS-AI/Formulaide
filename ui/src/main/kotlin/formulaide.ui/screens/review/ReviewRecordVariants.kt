package formulaide.ui.screens.review

import formulaide.api.data.*
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.Client
import formulaide.client.routes.review
import formulaide.ui.components.LoadingSpinner
import formulaide.ui.components.StyledButton
import formulaide.ui.components.cards.*
import formulaide.ui.components.useAsync
import formulaide.ui.reportExceptions
import formulaide.ui.useClient
import formulaide.ui.useConfig
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.classes
import formulaide.ui.utils.parseHtmlForm
import formulaide.ui.utils.printElement
import react.FC
import react.Props
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.img
import react.dom.html.ReactHTML.td
import react.useMemo
import react.useState
import kotlin.js.Date

internal external interface ReviewRecordVariantProps : ReviewRecordProps {
	var history: List<ParsedTransition>
	var showFullHistory: Boolean?
	var updateShowFullHistory: (Boolean) -> Unit
}

internal fun ReviewRecordVariantProps.expand() = setExpandedRecords { plus(record to true) }

internal fun ReviewRecordVariantProps.collapse() = setExpandedRecords { plus(record to false) }

internal val ReviewRecordCollapsed = FC<ReviewRecordVariantProps>("ReviewRecordCollapsed") { props ->
	val tdClasses = "first:pl-8 last:pr-8 py-2"
	val tdDivClasses = "mx-4"

	// If we are displaying all records, add a column that displays in which state the record currently is
	if (props.windowState == null) td {
		classes = tdClasses

		div {
			classes = tdDivClasses
			+props.record.state.displayName()
		}
	}

	val firstTransition = useMemo(props.history) { props.history.first { it.transition.previousState == null } }

	fun parsedAsSequence(p: ParsedField<*>): Sequence<ParsedField<*>> = sequence {
		yield(p)
		p.children?.let { child -> yieldAll(child.flatMap { parsedAsSequence(it) }) }
	}

	val parsedFields = useMemo(firstTransition.submission) {
		if (firstTransition.submission != null)
			firstTransition.submission.fields
				.asSequence()
				.flatMap { parsedAsSequence(it) }
				.toList()
		else emptyList()
	}

	if (firstTransition.submission == null) {
		+"Chargement de la saisie…"
		LoadingSpinner()
		return@FC
	}

	for ((key, _) in props.columnsToDisplay) {
		// Find the parsed value that corresponds to this column
		val parsedField = parsedFields
			.firstOrNull { it.fullKeyString == key }

		td {
			classes = tdClasses
			div {
				classes = tdDivClasses

				if (parsedField is ParsedList<*>) {
					+parsedField.children.mapNotNull { it.rawValue }
						.joinToString(separator = ", ")
				} else {
					+(parsedField?.rawValue ?: "")
				}
			}
		}
	}

	td {
		StyledButton {
			text = "▼"
			action = { props.expand() }
		}
	}
}

internal val ReviewRecordExpanded = FC<ReviewRecordVariantProps>("ReviewRecordExpanded") { props ->
	val state = props.windowState
	val (client) = useClient()
	require(client is Client.Authenticated) { "Seuls les employés peuvent accéder à cette page" }

	val scope = useAsync()

	val recordState = props.record.state
	val actionOrNull = (recordState as? RecordState.Action)?.current
	val nextAction = useMemo(props.form.actions, actionOrNull) {
		props.form.actions.indexOfFirst { actionOrNull?.id == it.id }
			.takeUnless { it == -1 }
			?.let { props.form.actions.getOrNull(it + 1) }
			?.let { RecordState.Action(it.createRef()) }
	}
	val (selectedDestination, updateSelectedDestination) = useState(nextAction ?: recordState)
		.asDelegated()

	val (reason, updateReason) = useState<String>()
		.asDelegated()

	val formCardId = "record-card-${props.record.id}"

	td {
		colSpan = props.columnsToDisplay.size

		if (props.windowState == null) Card {
			id = formCardId
			title = "Dossier"
			subtitle = props.record.state.displayName()

			reviewRecordButtons(props, formCardId)

			ReviewRecordPrintImages()
			ReviewRecordContents { +props }
		} else FormCard {
			id = formCardId
			title = "Dossier"

			reviewRecordButtons(props, formCardId)

			ReviewRecordPrintImages()
			ReviewRecordContents { +props }

			Footer {
				ReviewRecordDecision {
					+props
					this.reason = reason
					this.updateReason = updateReason
					this.selectedDestination = selectedDestination
					this.updateSelectedDestination = updateSelectedDestination
					this.nextAction = nextAction
				}
			}

			submit("Confirmer") { htmlForm ->
				val submission =
					if (state is RecordState.Action && state.current.obj.fields?.fields?.isNotEmpty() == true)
						parseHtmlForm(
							htmlForm,
							props.form,
							state.current.obj,
						)
					else null

				launch {
					client.review(ReviewRequest(
						props.record.createRef(),
						RecordStateTransition(
							(Date.now() / 1000).toLong(),
							state,
							selectedDestination,
							assignee = client.me.createRef(),
							reason = reason,
						),
						submission.takeIf { true },
					))
					scope.reportExceptions {
						props.refresh.current?.invoke()
					}

					updateReason { null }
				}
			}
		}
	}
}

private fun CardProps.reviewRecordButtons(props: ReviewRecordVariantProps, formCardId: String) {
	action("Réduire") { props.collapse() }

	action(if (props.showFullHistory == true) "Valeurs les plus récentes" else "Historique") {
		props.updateShowFullHistory(!(props.showFullHistory ?: false))
	}

	action("Récépissé") { printElement(formCardId) }
}

private const val printImageDiv = "flex grow shrink object-contain"
private const val printImage = "min-w-[20%] max-w-[50%] object-contain"

private val ReviewRecordPrintImages = FC<Props>("ReviewRecordPrintImages") {
	val config by useConfig()

	div {
		classes = "max-w-full gap-2 my-2 max-h-20 min-h-5 justify-between hidden print:flex"

		div {
			classes = "$printImageDiv justify-start"
			img {
				classes = printImage
				src = config?.pdfLeftImageURL
			}
		}

		div {
			classes = "$printImageDiv justify-end"
			img {
				classes = printImage
				src = config?.pdfRightImageURL
			}
		}
	}
}
