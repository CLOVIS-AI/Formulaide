# Module core-users

Authentication and role management for Formulaide users.

There are two types of [users][opensavvy.formulaide.core.User] of Formulaide: the end users, called [guests][opensavvy.formulaide.core.User.Role.Guest], and [employees][opensavvy.formulaide.core.User.Role.Employee].

Guests can submit data to the available forms. They are always anonymous and unauthenticated, and therefore cannot access any data (even their own).

Employees can also submit data to forms, as well as accessing internal forms.
Employees are part of [departments][opensavvy.formulaide.core.Department], which represent the different sections of the company or association using Formulaide.
End user submissions are protected to only be visible by employees belonging to specific departments.

The [administrator][opensavvy.formulaide.core.User.Role.Administrator] is a specific role given to employees who are allowed to manage Formulaide itself. They can create and edit forms, browse through all user data, and delete data permanently to comply with GDPR requests.
