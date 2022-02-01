package formulaide.ui.utils

import formulaide.ui.reportExceptions
import kotlinx.browser.document
import org.w3c.dom.HTMLIFrameElement

/**
 * Selects an element by its [elementId], copies it to an invisible `iframe`, and asks the browser to print it.
 */
fun printElement(elementId: String) = reportExceptions {
	val contents = document.getElementById(elementId)
		?: error("L'élément $elementId est introuvable, cela ne devrait pas être possible.")

	val frame = document.getElementById("ifmcontentstoprint")
		?: error("Impossible de trouver la iframe 'ifmcontentstoprint'.")

	val iframe = frame as? HTMLIFrameElement
		?: error("L'iframe a un type incorrect")

	val window = iframe.contentWindow
		?: error("Impossible d'ouvrir la fenêtre de l'iframe.")

	with(window.document) {
		open()
		write(contents.innerHTML)
		close()
	}

	with(window) {
		focus()
		print()
	}
}
