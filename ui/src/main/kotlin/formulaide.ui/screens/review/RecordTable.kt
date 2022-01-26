package formulaide.ui.screens.review

import formulaide.api.data.Record
import formulaide.api.fields.FormField
import formulaide.api.fields.SimpleField
import formulaide.api.fields.asSequenceWithKey
import formulaide.ui.CrashReporter
import react.FC
import react.RefObject
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.table
import react.dom.html.ReactHTML.tbody
import react.dom.html.ReactHTML.td
import react.dom.html.ReactHTML.th
import react.dom.html.ReactHTML.thead
import react.dom.html.ReactHTML.tr
import react.key
import react.useMemo

external interface RecordTableProps : ReviewProps {
	var records: List<Record>
	var expandedRecords: Map<Record, Boolean>
	var setExpandedRecords: (Map<Record, Boolean>.() -> Map<Record, Boolean>) -> Unit

	var refresh: RefObject<suspend () -> Unit>
}

val RecordTable = FC<RecordTableProps>("RecordTable") { props ->
	val columnsToDisplay = useMemo(props.form.mainFields) {
		props.form.mainFields.asSequenceWithKey()
			.filter { (_, it) -> it !is FormField.Composite }
			.filter { (_, it) -> it !is FormField.Simple || it.simple != SimpleField.Message }
			.toList()
	}

	table {
		className = "table-auto w-full"

		if (props.expandedRecords.values.any { !it }) { // Display the column titles if at least 1 record is collapsed
			thead {
				tr {
					val thClasses = "first:pl-8 last:pr-8 py-2"

					if (props.windowState == null) th {
						className = thClasses
						div {
							className = "mx-4"
							+"Étape"
						}
					}

					columnsToDisplay.forEach { (_, it) ->
						th {
							className = thClasses
							div {
								className = "mx-4"
								+it.name
							}
						}
					}
				}
			}
		}

		tbody {
			for (record in props.records) {
				CrashReporter {
					ReviewRecord {
						+props
						this.record = record
						this.columnsToDisplay = columnsToDisplay

						key = record.id
					}
				}
			}

			if (props.records.isEmpty()) {
				td {
					colSpan = columnsToDisplay.size
					div {
						className = "flex justify-center items-center w-full h-full"
						p {
							className = "my-10"
							+"Aucun résultat"
						}
					}
				}
			}
		}
	}
}
