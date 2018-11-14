/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.apilint

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec

class ApiLintPlugin implements Plugin<Project> {
    void apply(Project project) {
        def extension = project.extensions.create('apiLint', ApiLintPluginExtension)

        Plugin android = project.getPlugins().findPlugin('android-library')

        if (!android) {
            throw new GradleException('You need the Android Library plugin to run ApiLint.')
        }

        // TODO: support applications
        project.android.libraryVariants.all { variant ->
            def name = variant.name.capitalize()
            def apiFile = project.file(
                        "${variant.javaCompile.destinationDir}/${extension.apiOutputFileName}")

            def currentApiFile = project.file(extension.currentApiRelativeFilePath)

            def apiGenerate = project.task("apiGenerate${name}", type: ApiCompatLintTask) {
                description = "Generates API file for build variant ${name}"
                doFirst {
                    classpath = project.files(variant.getJavaCompile().classpath.files)
                }

                source = variant.getJavaCompile().source
                exclude '**/R.java', '**/BuildConfig.java'

                outputFile = apiFile
                packageFilter = extension.packageFilter
                destinationDir = new File(destinationDir, variant.baseName)
                sourcePath = variant.sourceSets.collect({ it.javaDirectories }).flatten()
            }

            apiGenerate.dependsOn variant.javaCompile

            def apiLint = project.task("apiLint${name}", type: PythonExec) {
                description = "Runs API lint checks for variant ${name}"
                group = 'Verification'
                workingDir '.'
                scriptPath 'apilint.py'
                args '--show-noticed'
                args apiFile
                args currentApiFile
            }

            apiLint.dependsOn apiGenerate
            project.tasks.check.dependsOn apiLint

            def apiDiff = project.task("apiDiff${name}", type: Exec) {
                description = "Prints the diff between the existing API and the local API."
                workingDir '.'
                commandLine 'diff'
                args '-U5'
                args currentApiFile
                args '--label', 'Existing API'
                args apiFile
                args '--label', 'Local API'

                // diff exit value is != 0 if the files are different
                ignoreExitValue true
            }

            def apiLintHelp = project.task("apiLintHelp${name}") {
                description = "Prints help for when an API change is detected."
                onlyIf {
                    apiLint.state.failure != null
                }
                doLast {
                    println ""
                    println "The API has been modified. If the changes look correct, please run"
                    println ""
                    println "\$ ./gradlew apiUpdateFile${name}"
                    println ""
                    println "to update the API file."
                }
            }

            apiLintHelp.dependsOn apiDiff
            apiLint.finalizedBy apiLintHelp

            def apiUpdate = project.task("apiUpdateFile${name}", type: Copy) {
                description = "Updates the API file from the local one for variant ${name}"
                group = 'Verification'
                from apiFile
                into currentApiFile.getParent()
                rename { apiFile.getName() }
            }

            apiUpdate.dependsOn apiGenerate
        }
    }
}
