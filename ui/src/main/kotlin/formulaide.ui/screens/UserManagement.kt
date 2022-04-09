package formulaide.ui.screens

import formulaide.api.types.Email
import formulaide.api.types.Ref
import formulaide.api.types.Ref.Companion.createRef
import formulaide.api.users.NewUser
import formulaide.api.users.Service
import formulaide.api.users.User
import formulaide.client.Client
import formulaide.client.routes.createUser
import formulaide.client.routes.editUser
import formulaide.client.routes.listUsers
import formulaide.ui.*
import formulaide.ui.components.StyledButton
import formulaide.ui.components.cards.Card
import formulaide.ui.components.cards.FormCard
import formulaide.ui.components.cards.action
import formulaide.ui.components.cards.submit
import formulaide.ui.components.inputs.*
import formulaide.ui.components.text.LightText
import formulaide.ui.components.useAsync
import formulaide.ui.utils.DelegatedProperty.Companion.asDelegated
import formulaide.ui.utils.classes
import formulaide.ui.utils.traceRenders
import formulaide.ui.utils.useEquals
import formulaide.ui.utils.useListEquality
import react.*
import react.dom.html.InputType
import react.dom.html.ReactHTML.br
import react.dom.html.ReactHTML.div
import react.dom.html.ReactHTML.hr
import react.dom.html.ReactHTML.option

val UserList = FC<Props>("UserList") {
	traceRenders("UserList")

	val scope = useAsync()

	val (client) = useClient()
	require(client is Client.Authenticated) { "Seul un administrateur a le droit de voir la liste des utilisateurs" }
	val (me) = useUser()

	if (me == null) {
		Card {
			title = "Employés"
			loading = true
			+"Récupération de l'utilisateur…"
		}
		return@FC
	}

	var listDisabledUsers by useState(false)
	var users by useState(emptyList<User>()).asDelegated()
		.useListEquality()
		.useEquals()
	useEffect(client, listDisabledUsers) {
		scope.reportExceptions {
			users = client.listUsers(listDisabledUsers)
		}
	}

	val services by useServices()
	var filterByServices by useState(false)
	var filteredServices by useState(emptySet<Service>())

	val filteredUsers = useMemo(users, filteredServices, filterByServices) {
		val selectedUsers = if (!filterByServices) users
		else users.filter { it.service.id in filteredServices.map { it.id } }

		selectedUsers.sortedBy { it.fullName }
	}

	Card {
		title = "Employés"
		action("Créer un employé") { navigateTo(Screen.NewUser) }
		loading = users.isEmpty()

		Field {
			id = "hide-disabled"
			text = "Utilisateurs désactivés"

			Checkbox {
				id = "hide-disabled"
				text = "Afficher les comptes désactivés"
				checked = listDisabledUsers
				onChange = { listDisabledUsers = it.target.checked }
			}
		}

		Field {
			id = "service-filters"
			text = "Choisir les services à afficher"

			Checkbox {
				id = "service-filters"
				text = "Ne pas filtrer par service"
				checked = !filterByServices
				onChange = { filterByServices = !it.target.checked }
			}

			if (filterByServices)
				for (service in services) {
					br()

					Checkbox {
						id = "service-filter-${service.id}"
						text = "Afficher le service « ${service.name} »"
						checked = service in filteredServices
						onChange = {
							@Suppress("SuspiciousCollectionReassignment") // +=/-= copy the set and call the setter, this is intended here (React setState)
							if (it.target.checked)
								filteredServices += service
							else
								filteredServices -= service
						}
					}
				}
		}

		for (user in filteredUsers) {
			hr {
				classes = "mb-2"
			}

			FormField {
				div { // Name and email
					+"${user.fullName} "
					LightText { text = user.email.email }
				}

				div { // Services
					user.service.loadFrom(services)
					LightText { text = "Service : " }

					ControlledSelect {
						for (service in services) {
							Option(service.name, service.id, user.service.id == service.id) {
								editUser(user, client, service = service) { newUser ->
									users = users - user + newUser
								}

								if (user == me) {
									forceTokenRefresh(client)
								}
							}
						}
					}
				}

				div { // Role
					LightText { text = "Rôle : ${if (user.administrator) "Administration" else "Employé"}" }

					if (user != me) StyledButton {
						text = "Modifier"
						action = {
							editUser(user, client, administrator = !user.administrator) { newUser ->
								users = users - user + newUser
							}
						}
					}
				}

				div { // buttons
					if (user != me) {
						StyledButton {
							text = if (user.enabled) "Désactiver" else "Activer"
							action = {
								editUser(user, client, enabled = !user.enabled) { newUser ->
									users = users - user + newUser
								}
							}
						}
					}

					StyledButton {
						text = "Modifier le mot de passe"
						action = {
							navigateTo(Screen.EditPassword(user.email, Screen.ShowUsers))
						}
					}
				}
			}
		}
	}
}

private suspend fun editUser(
	user: User,
	client: Client.Authenticated,
	enabled: Boolean? = null,
	administrator: Boolean? = null,
	service: Service? = null,
	onChange: (User) -> Unit,
) {
	val newUser = client.editUser(user, enabled, administrator, service?.createRef())
	onChange(newUser)
}

val CreateUser = FC<Props>("CreateUser") {
	val services by useServices()

	var email by useState("")
	var fullName by useState("")
	var selectedService by useState(services.firstOrNull())
	var admin by useState(false)
	var password1 by useState("")
	var password2 by useState("")

	val (client) = useClient()
	require(client is Client.Authenticated) { "Un employé anonyme ne peut pas créer d'utilisateurs" }

	FormCard {
		title = "Ajouter un employé"

		submit("Créer") {
			require(password1 == password2) { "Les deux mots de passes ne correspondent pas." }

			require(password1.isNotBlank()) { "Le mot de passe ne peut pas être vide." }
			require(email.isNotBlank()) { "L'adresse mail ne peut pas être vide, trouvé '$email'" }
			requireNotNull(selectedService) { "Un utilisateur doit appartenir à un service, mais aucun n'a été choisi" }

			launch {
				client.createUser(NewUser(
					password1,
					User(
						email = Email(email),
						fullName = fullName,
						service = Ref(selectedService!!),
						administrator = admin
					)
				))

				navigateTo(Screen.ShowUsers)
			}
		}

		Field {
			id = "employee-email"
			text = "Adresse mail"

			Input {
				type = InputType.email
				id = "employee-email"
				required = true
				value = email
				onChange = { email = it.target.value }
			}
		}

		Field {
			id = "employee-name"
			text = "Nom affiché"

			Input {
				type = InputType.text
				id = "employee-name"
				required = true
				value = fullName
				onChange = { fullName = it.target.value }
			}
		}

		Field {
			id = "employee-service"
			text = "Service"

			Select {
				onSelection = { option -> selectedService = services.find { it.id == option.value } }

				for (service in services) {
					option {
						+service.name
						value = service.id
					}
				}
			}
		}

		Field {
			id = "employee-is-admin"
			text = "Droits"

			Checkbox {
				id = "employee-is-admin"
				text = "Cet employé est un administrateur"
				checked = admin
				onChange = { admin = it.target.checked }
			}
		}

		Field {
			id = "employee-password-1"
			text = "Mot de passe"

			Input {
				type = InputType.password
				id = "employee-password-1"
				required = true
				value = password1
				onChange = { password1 = it.target.value }
				minLength = 6
			}
		}

		Field {
			id = "employee-password-2"
			text = "Confirmer le mot de passe"

			Input {
				type = InputType.password
				id = "employee-password-2"
				required = true
				value = password2
				onChange = { password2 = it.target.value }
				minLength = 6
			}
		}
	}
}
