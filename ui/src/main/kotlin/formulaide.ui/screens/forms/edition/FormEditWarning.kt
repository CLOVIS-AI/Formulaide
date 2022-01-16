package formulaide.ui.screens.forms.edition

import formulaide.ui.components.text.ErrorText
import react.FC
import react.Props
import react.dom.html.ReactHTML.li
import react.dom.html.ReactHTML.p
import react.dom.html.ReactHTML.ul

val FormEditWarning = FC<Props>("FormEditWarning") {
	ErrorText {
		text =
			" Il est possible que le système refuse certaines modifications. Il est possible que le système autorise des modifications qui amènent à l'inaccessibilité de certaines données."
	}
	+" Aucune garantie n'est donnée pour les modifications non listées ci-dessous."

	p {
		className = "pt-2"

		+"Modifications qui ne peuvent pas causer de problèmes :"
	}
	ul {
		className = "list-disc"

		li { +"Renommer le formulaire" }
		li { +"Renommer un champ ou une étape" }
		li { +"Modifier l'ordre de plusieurs champs" }
		li { +"Modifier la valeur par défaut d'un champ" }
		li { +"Modifier le service responsable d'une étape" }
		li { +"Créer un champ facultatif (si aucun champ n'a été supprimé)" }
	}

	p {
		className = "pt-2"

		+"Modifications qui peuvent être refusées :"
	}
	ul {
		className = "list-disc"

		li { +"Modifier les restrictions des champs (obligatoire, facultatif, longueur maximale…)" }
		li { +"Modifier le type d'un champ (dans certains cas, il n'est pas possible d'annuler la modification)" }
		li { +"Créer un champ facultatif (si un champ avait été supprimé précédemment)" }
	}

	p {
		className = "pt-2"

		+"Modifications qui peuvent amener à une perte de données :"
	}
	ul {
		className = "list-disc pb-4"

		li { +"Modifier le type d'un champ" }
		li { +"Supprimer un champ" }
	}
}
