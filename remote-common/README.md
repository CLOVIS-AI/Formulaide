# Module remote-common

HTTP API declaration of the various endpoints of the Formulaide project.

This module exports the declarations from `core` to remote users (the first-party web UI as well third-party implementations). It has very little logic, so everything documented in `core` is not repeated here; when in doubt about the usage of some API object, please see the matching entry in `core`.

## Reading order

The entrypoint is the [Api2][opensavvy.formulaide.remote.Api2] class. To learn more about authentication, read the documentation of the `backend` project.

# Package opensavvy.formulaide.remote

Declaration of the API structure.

# Package opensavvy.formulaide.remote.dto

Declaration of the objects used as request and response bodies by the API.
