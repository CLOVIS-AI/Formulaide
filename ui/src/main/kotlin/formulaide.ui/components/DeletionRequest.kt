package formulaide.ui.components

import formulaide.api.data.Record
import formulaide.api.types.Ref.Companion.createRef
import formulaide.client.Client
import formulaide.client.routes.deleteRecord
import formulaide.client.routes.requestDeleteRecord
import formulaide.ui.components.inputs.Field
import formulaide.ui.components.inputs.Input
import formulaide.ui.components.text.ErrorText
import formulaide.ui.useClient
import formulaide.ui.utils.classes
import io.ktor.client.plugins.*
import react.FC
import react.Props
import react.dom.html.InputType
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.p
import react.useState

private const val MAX_ATTEMPTS = 3

internal external interface DeletionRequestProps : Props {
	var delete: Boolean?
	var onFinished: suspend () -> Unit
	var record: Record
}

internal val DeletionRequest = FC<DeletionRequestProps>("DeletionRequest") { props ->
	var challenge by useState<String>()
	var response by useState<String>()

	var failureCounter by useState(0)

	val (client) = useClient()
	require(client is Client.Authenticated) { "Les utilisateurs non-connectés ne peuvent pas supprimer des dossiers" }
	require(client.me.administrator) { "Les non-administrateurs ne peuvent pas supprimer des dossiers" }

	useAsyncEffect(failureCounter) {
		challenge = client.requestDeleteRecord(props.record.createRef()).challenge
	}

	div {
		classes = "mb-4 bg-orange-200 rounded-lg p-2"

		p {
			classes = "mb-2 mx-1"

			+"Voulez-vous vraiment supprimer cette donnée ? Il ne sera pas possible de revenir en arrière. "
			+"Si vous souhaitez cacher cette donnée parce qu'elle est invalide, annulez puis utilisez le bouton « refuser »."
		}

		p {
			classes = "mb-2 mx-1"

			+"Pour confirmer, merci de répondre à cette question :"
		}

		p {
			classes = "mb-2 mx-1"
			if (challenge == null) {
				LoadingSpinner()
			} else {
				Field {
					text = challenge!!
					id = "record-${props.record}-delete-challenge"

					Input {
						type = InputType.text
						id = "record-${props.record}-delete-challenge"
						autoFocus = true
						required = true
						value = response
						onChange = { response = it.target.value }
					}
				}
			}
		}

		div {
			when (failureCounter) {
				in 0 until MAX_ATTEMPTS -> StyledButton {
					text = "Supprimer définitivement"
					action = {
						try {
							client.deleteRecord(
								props.record.createRef(),
								requireNotNull(response) { "Vous n'avez pas rempli la question demandée." }
							)

							props.onFinished()
						} catch (e: ClientRequestException) {
							if ("La réponse est incorrecte" in e.message)
								failureCounter++
							else
								throw e
						}
					}
					emphasize = false
					enabled = challenge != null
				}

				else -> ErrorText {
					text = "Trop de tentatives échouées"
				}
			}

			StyledButton {
				text = "Annuler"
				action = { props.onFinished() }
				emphasize = true
			}
		}

		if (failureCounter > 0) {
			ErrorText {
				text = "Mauvaise réponse. ${MAX_ATTEMPTS - failureCounter} tentative(s) restante(s)."
			}
		}
	}
}
