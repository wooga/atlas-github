/*
 * Copyright 2018 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.github

import java.nio.charset.StandardCharsets

class GithubIntegrationSpec extends GithubPublishIntegrationWithDefaultAuth {

    def setup() {
        buildFile << """
            import wooga.gradle.github.base.tasks.Github
            github.repositoryName = "$testRepositoryName"

            task customGithubTask(type:Github)
        """.stripIndent()
    }

    def "executes github tasks without failure when actions are empty"() {
        expect:
        runTasksSuccessfully("customGithubTask")
    }

    def "can access repository from action"() {
        given: "a action inside customGithubTask which makes API calls with the repository object"

        buildFile << """
            customGithubTask {
                doLast {
                    println("this is the current repo name " + repository.full_name)
                }
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("customGithubTask")

        then:
        result.standardOutput.contains("this is the current repo name " + testRepositoryName)
    }

    def "can call API calls with repository object from action"() {
        given: "a action inside customGithubTask which makes API calls with the repository object"
        buildFile << """
            customGithubTask {
                doLast {
                    repository.createLabel("TestLabel", "ffffff")
                }
            }
        """.stripIndent()

        when:
        runTasksSuccessfully("customGithubTask")

        then:
        def l = testRepo.getLabel("TestLabel")

        cleanup:
        l.delete()
    }

    def "can update files and content of repository"() {
        given: "a test file in the created repo"
        createContent(initialContent, "add empty release notes", file)


        and: "an action inside customGithubTask which changes this new file"
        buildFile << """
            customGithubTask {
                doLast {
                    def content = repository.getFileContent("$file")
                    content.update("$updatedContent", "update release notes")
                }
            }
        """.stripIndent()

        when:
        runTasksSuccessfully("customGithubTask")

        then:
        def content = testRepo.getFileContent(file)
        InputStream contentStream = content.read()
        InputStreamReader contentStreamReader = new InputStreamReader(contentStream, StandardCharsets.UTF_8)
        contentStreamReader.text == updatedContent

        cleanup:
        content.delete("delete release notes")

        where:
        file = "RELEASE_NOTES.md"
        initialContent = "## EMPTY RELEASE NOTES"
        updatedContent = "## Initial Release"
    }

    def "can access github client object"() {
        given: "an action inside customGithubTask which creates a new repo"
        buildFile << """
            customGithubTask {
                doLast {
                    def builder = client.createRepository("$customRepositoryName".split('/')[1])
                    builder.description("$description")
                    builder.autoInit(false)
                    builder.licenseTemplate('MIT')
                    builder.private_(false)
                    builder.issues(false)
                    builder.wiki(false)
                    builder.create()
                }
            }
        """.stripIndent()

        and:
        maybeDelete(customRepositoryName)

        when:
        runTasksSuccessfully("customGithubTask")

        then:
        def customRepo = client.getRepository(customRepositoryName)
        customRepo.description == description

        cleanup:
        maybeDelete(customRepositoryName)

        where:
        customRepositoryName = testRepositoryName + "_custom"
        description = "Custom repo created via gradle"
    }

    def "fails when repo is not available"() {
        given: "a buildfile with publish task and non existing repo"
        buildFile << """
            customGithubTask {
                repositoryName = "${testUserName}/customRepo"
                doLast {
                    repository.full_name
                }
            }
        """

        expect:
        def result = runTasksWithFailure("customGithubTask")
        outputContains(result, "can't find repository $testUserName/customRepo")
    }
}
