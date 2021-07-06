package formulaide.ui.screens

import formulaide.api.types.Email
import formulaide.api.users.NewUser
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.createUser
import formulaide.ui.Screen
import formulaide.ui.ScreenProps
import formulaide.ui.components.*
import formulaide.ui.launchAndReportExceptions
import formulaide.ui.utils.text
import kotlinx.html.InputType
import kotlinx.html.js.onSubmitFunction
import org.w3c.dom.HTMLInputElement
import react.dom.attrs
import react.dom.option
import react.functionalComponent
import react.useRef
import react.useState

val UserList = functionalComponent<ScreenProps> { props ->
	styledCard(
		"Employés",
		null,
		"Créer un employé" to { props.navigateTo(Screen.NewUser) }
	) {
		text("Ici, dans le futur : la liste des utilisateurs") //TODO in #79, #76
	}
}

val CreateUser = functionalComponent<ScreenProps> { props ->
	val email = useRef<HTMLInputElement>()
	val fullName = useRef<HTMLInputElement>()
	val (selectedService, setSelectedService) = useState(props.services.firstOrNull())
	val admin = useRef<HTMLInputElement>()
	val password1 = useRef<HTMLInputElement>()
	val password2 = useRef<HTMLInputElement>()

	val client = props.client
	require(client is Client.Authenticated) { "Un employé anonyme ne peut pas créer d'utilisateurs" }

	styledFormCard(
		"Ajouter un employé",
		null,
		"Créer",
		contents = {
			styledField("employee-email", "Adresse mail") {
				styledInput(InputType.email, "employee-email", required = true, ref = email)
			}

			styledField("employee-name", "Nom affiché") {
				styledInput(InputType.text, "employee-name", required = true, ref = fullName)
			}

			styledField("employee-service", "Service") {
				styledSelect(onSelect = { option -> setSelectedService(props.services.find { it.id.toString() == option.value }) }) {
					for (service in props.services) {
						option {
							text(service.name)
							attrs {
								value = service.id.toString()
							}
						}
					}
				}
			}

			styledField("employee-is-admin", "Droits") {
				styledCheckbox("employee-is-admin",
				               "Cet employé est un administrateur",
				               ref = admin)
			}

			styledField("employee-password-1", "Mot de passe") {
				styledInput(InputType.password,
				            "employee-password-1",
				            required = true,
				            ref = password1) {
					minLength = "5"
				}
			}

			styledField("employee-password-2", "Confirmer le mot de passe") {
				styledInput(InputType.password,
				            "employee-password-2",
				            required = true,
				            ref = password2) {
					minLength = "5"
				}
			}
		}
	) {
		onSubmitFunction = {
			it.preventDefault()

			launchAndReportExceptions(props) {
				val password1Value = password1.current?.value
				val password2Value = password2.current?.value

				require((password1Value
					?: Unit) == password2Value) { "Les deux mots de passes ne correspondent pas." }

				val passwordOrFail = (password1Value
					?: error("Le mot de passe ne peut pas être vide, trouvé $password1"))

				val emailOrFail = (email.current?.value
					?: error("L'adresse mail ne peut pas être vide, trouvé $email"))

				val fullNameOrFail = (fullName.current?.value
					?: error("Le nom ne peut pas être vide, trouvé $fullName"))

				val serviceOrFail = (selectedService?.id
					?: error("L'utilisateur doit avoir choisi un service, trouvé $selectedService"))

				val adminOrFail = admin.current?.value
					?: error("Il faut préciser si l'utilisateur est un administrateur ou non, trouvé $admin")

				client.createUser(NewUser(
					passwordOrFail,
					User(
						email = Email(emailOrFail),
						fullName = fullNameOrFail,
						service = serviceOrFail,
						administrator = adminOrFail == "on"
					)
				))

				props.navigateTo(Screen.ShowUsers)
			}
		}
	}
}
