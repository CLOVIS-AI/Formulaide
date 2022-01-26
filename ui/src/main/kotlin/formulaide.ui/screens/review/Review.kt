package formulaide.ui.screens.review

import formulaide.api.data.Composite
import formulaide.api.data.Form
import formulaide.api.data.RecordState
import formulaide.client.Client
import formulaide.client.routes.compositesReferencedIn
import formulaide.ui.components.LoadingSpinner
import formulaide.ui.components.useAsync
import formulaide.ui.components.useAsyncEffect
import formulaide.ui.screens.forms.list.getRecords
import formulaide.ui.useClient
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.onSet
import formulaide.ui.utils.useEquals
import formulaide.ui.utils.useListEquality
import react.*
import react.dom.html.ReactHTML.div

@Suppress("FunctionName")
fun Review(form: Form, state: RecordState?) = FC<Props>("ReviewWrapper") {
	Review {
		this.form = form
		this.windowState = state
	}
}

external interface ReviewProps : Props {
	var form: Form

	/**
	 * The state this review corresponds to.
	 *
	 * `null` means that this page is a list of all records in this form, not a review.
	 */
	var windowState: RecordState?

	var referencedComposites: List<Composite>
}

/**
 * The review screen allows employees to accept and refuse records submitted by users.
 */
val Review = FC<ReviewProps>("Review") { props ->
	val (client) = useClient()
	require(client is Client.Authenticated) { "Unauthenticated users cannot review records." }

	val scope = useAsync()
	val cachedRecords = useMemo { scope.getRecords(client, props.form) }

	val (openedRecords, setOpenedRecords) = useState(cachedRecords.associateWith { true })
		.asDelegated()
		.useEquals()

	val (records, updateRecords) = useState(cachedRecords)
		.asDelegated()
		.onSet { newRecords ->
			val opened = openedRecords.toMutableMap()
			for (record in newRecords)
				if (record !in opened.keys)
					opened[record] = true
			setOpenedRecords { opened }
		}
		.useListEquality()
		.useEquals()

	var referencedComposites by useState(emptyList<Composite>())
		.asDelegated()
		.useListEquality()
		.useEquals()

	var formIsLoaded by useState(false)
	useAsyncEffect(props.form) {
		val referenced = client.compositesReferencedIn(props.form)
		props.form.load(referenced)
		referencedComposites = referenced
		formIsLoaded = true
	}

	val refresh =
		useRef<suspend () -> Unit> { console.error("Review.refresh called, but the ref has not been loaded yet.") }

	if (!formIsLoaded) {
		+"Chargement du formulaire en cours…"
		LoadingSpinner()
		return@FC
	}

	div {
		className = "lg:grid lg:grid-cols-3 lg:gap-y-0"

		div {
			className = "lg:order-2"

			SearchBar {
				+props
				this.records = records
				this.updateRecords = updateRecords
				this.setOpenedRecords = setOpenedRecords
				this.refresh = refresh

				key = "search-bar"
			}
		}

		div {
			className = "lg:col-span-2 lg:order-1 w-full overflow-x-auto"

			RecordTable {
				+props
				this.records = records
				this.expandedRecords = openedRecords
				this.setExpandedRecords = setOpenedRecords
				this.referencedComposites = referencedComposites
				this.refresh = refresh
			}
		}
	}
}
