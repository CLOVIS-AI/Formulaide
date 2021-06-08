package formulaide.client

// Implementation of the workaround mentioned here:
// https://youtrack.jetbrains.com/issue/KT-22228
expect fun runTest(block: suspend () -> Unit)
