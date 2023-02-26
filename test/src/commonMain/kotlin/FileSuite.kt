package opensavvy.formulaide.test

import opensavvy.formulaide.core.File
import opensavvy.formulaide.test.execution.Factory
import opensavvy.formulaide.test.execution.Suite

class FileTestData(
	val files: File.Service,
)

// TODO in #267: scenarios
//  - upload a file
//    - it can be accessed, but not read
//    - if we wait TTL_UNLINKED, it is deleted
//    - its expireAfter is correct
//    - the mime type is as expected
//    - the content is the same
//    - the origin is null
//  - link it (optional suite)
//    - if we wait TTL_UNLINKED, it's not deleted
//    - if we wait the length of time configured in the form, it is deleted
//    - it can be accessed & read
//    - the origin is correct
//    - it's not possible to link it if the field ID doesn't match
//    - it's not possible to link it if the submission doesn't match
//    - it's not possible to link it if the record doesn't match
//  - create a matching submission (same battery as the link)

fun Suite.fileTestSuite(
	data: Factory<FileTestData>,
) {

}
