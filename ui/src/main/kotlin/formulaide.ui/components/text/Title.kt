package formulaide.ui.components.text

import formulaide.ui.components.LoadingSpinner
import react.FC
import react.Props
import react.dom.html.ReactHTML.h2

external interface TitleProps : Props {
	var title: String

	/**
	 * If this card is currently loading, a [LoadingSpinner] will be displayed near the [title].
	 */
	var loading: Boolean?
}

val Title = FC<TitleProps>("Title") { props ->
	h2 {
		className = "text-xl"

		+props.title
		if (props.loading == true) LoadingSpinner()
	}
}
