package formulaide.ui.screens

import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.utils.text
import kotlinx.html.js.onClickFunction
import react.dom.attrs
import react.dom.button
import react.dom.li
import react.dom.ul
import react.functionalComponent

val FormList = functionalComponent<ScreenProps> { props ->
	val forms = props.forms

	ul {
		for (form in forms) {
			var text = form.name

			if (!form.public)
				text += " • Ce formulaire est interne"

			li { text(text) }
			button {
				text("Saisir")
				attrs {
					onClickFunction = { props.navigateTo(Screen.SubmitForm(form)) }
				}
			}
		}
	}

}
