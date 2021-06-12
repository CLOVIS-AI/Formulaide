package formulaide.ui

import formulaide.api.data.CompoundDataField
import formulaide.api.data.Data
import formulaide.api.data.NewCompoundData
import formulaide.api.users.PasswordLogin
import formulaide.client.Client
import formulaide.client.routes.createData
import formulaide.client.routes.login
import formulaide.ui.utils.detectTests
import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import react.dom.h1
import react.dom.render

val helloWorld get() = "Hello World!"

fun main() {
	if (detectTests())
		return

	render(document.getElementById("root")) {
		h1 {
			+helloWorld
		}
	}
}
