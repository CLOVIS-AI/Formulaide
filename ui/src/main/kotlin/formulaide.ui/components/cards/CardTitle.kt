package formulaide.ui.components.cards

import formulaide.ui.components.text.LightText
import formulaide.ui.components.text.Title
import formulaide.ui.components.text.TitleProps
import react.FC

external interface CardTitleProps : TitleProps {
	var subtitle: String?
}

val CardTitle = FC<CardTitleProps>("CardTitle") { props ->
	Title { +props }

	val secondary = props.subtitle
	if (secondary != null)
		LightText { text = secondary }
}
