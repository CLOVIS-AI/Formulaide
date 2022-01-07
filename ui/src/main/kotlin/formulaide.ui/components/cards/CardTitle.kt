package formulaide.ui.components.cards

import formulaide.ui.components.text.LightText
import formulaide.ui.components.text.Title
import formulaide.ui.components.text.TitleProps
import react.FC

external interface StyledCardTitleProps : TitleProps {
	var secondary: String?
}

val StyledCardTitle = FC<StyledCardTitleProps>("CardTitle") { props ->
	Title { +props }

	val secondary = props.secondary
	if (secondary != null)
		LightText { text = secondary }
}
