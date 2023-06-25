# Module core

Core APIs for the Formulaide project.

Formulaide administrators can create web forms entirely online, and declare how the data submitted by users is shared between departments of the company to ensure each request is checked by the correct people.

## Form structure

The information requested from the end user is represented by the [Form][opensavvy.formulaide.core.Form] class. The specific data requested is stored as a single [field][opensavvy.formulaide.core.Field], which recursively includes other fields to satisfy various use cases.

Forms are versioned to ensure data integrity; they describe how submissions made by end users are verified and stored.
Each [step][opensavvy.formulaide.core.Form.Step] is assigned to employees from a specific department, allowing them to make decisions about the request.

As fields can grow quite complex, it is possible to extract them into [templates][opensavvy.formulaide.core.Template], allowing to reuse them.
A template can be [imported into][opensavvy.formulaide.core.Field.importedFrom] a field of a form, or of another template. Using templates, employees can search for submissions created by an end user across multiple forms. Like forms, templates are versioned; however, unlike forms, it is not possible to submit data to templates, and they do not have review steps information.

## Submission and review

As forms and fields represent the structure of the requested data, other data structures represent the actual answers provided by the user.

[Submissions][opensavvy.formulaide.core.Submission] store the answers provided to a given field. They cannot be modified after creation to avoid tampering. [Records][opensavvy.formulaide.core.Record] are the counterpart to forms: they keep track of which review step a request is currently in, as well as the entire history of the linked decisions by employees and the related additional submissions, if any. Records can be searched to find specific requests.

# Package opensavvy.formulaide.core

Core objects of the project, represented by an immutable data structure, a companion reference, and a companion service.

The companion service is an interface that describes the contract all Formulaide implementations must respect to ensure proper interaction between components. Multiple first-party implementations are provided, most notably a MongoDB-based implementation (in the `mongo` project) and an API that delegates over HTTP (in the `remote` project). Service calls are aggressively cached to reduce network traffic.

Because each object is immutable, the companion reference is used to access their values over time. This avoids data nesting in other data structures, and makes it easier for a client to only request information they do not already possess.

For more information about this design pattern, see the [Pedestal](https://gitlab.com/opensavvy/pedestal) project.
