/*
 * Copyright 2018-2021 Wooga GmbH
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

import org.junit.Rule
import org.junit.contrib.java.lang.system.EnvironmentVariables
import org.junit.contrib.java.lang.system.RestoreSystemProperties
import spock.lang.Ignore
import spock.lang.Unroll

@Ignore
class GithubAuthenticationIntegrationSpec extends AbstractGithubIntegrationSpec {

    def setup() {
        buildFile << """
            github.repositoryName = "$testRepositoryName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                tagName = "v0.${Math.abs(new Random().nextInt() % 1000) + 1}.0-GithubAuthenticationIntegrationSpec"
            }
        """.stripIndent()
    }

    @Unroll
    def "can override authentication parameters in GithubPublish task with #authMethod"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with broken auth"
        buildFile << """
            github.token = "faketoken"
        """.stripIndent()

        and: "task runs without errors"
        def results = runTasksWithFailure("testPublish")
        assert outputContains(results, "Bad credentials")

        when: "overriding github access parameters"
        cleanupReleases()
        buildFile << """
            testPublish {
                $accessParams
            }
        """.stripIndent()

        then:
        runTasksSuccessfully("testPublish")

        where:
        accessParams                                                                         | authMethod
        "username = '${testUserName}'; password = '${testUserToken}'"                        | "username/password"
        "username = '${testUserName}'; token = '${testUserToken}'"                           | "username/token"
        "token = '${testUserToken}'"                                                         | "token"
    }

    @Unroll
    def "can set authentication in gradle properties with #authMethod"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "changing home variable"
        System.setProperty("user.home", projectDir.path)

        and: "task with errors"
        assert runTasksWithFailure("testPublish")

        when: "setting auth properties"
        createFile("gradle.properties") << accessParams

        then:
        runTasksSuccessfully("testPublish")

        where:
        accessParams                                                          | authMethod
        "github.username=${testUserName}\ngithub.password=${testUserToken}\n" | "username/password"
        "github.username=${testUserName}\ngithub.token=${testUserToken}\n"    | "username/token"
        "github.token=${testUserToken}\n"                                     | "token"
    }

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties()

    @Unroll
    def "can set authentication in ~/.github with #authMethod"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "changing home variable"
        System.setProperty("user.home", projectDir.path)

        and: "task with errors"
        assert runTasksWithFailure("testPublish")

        when: "setting auth properties in .github"
        createFile(".github") << accessParams

        and: "changing home variable"
        System.setProperty("user.home", projectDir.path)

        then:
        runTasksSuccessfully("testPublish")

        where:
        accessParams                                       | authMethod
        "login=${testUserName}\npassword=${testUserToken}" | "username/password"
        "login=${testUserName}\noauth=${testUserToken}"    | "username/token"
        "oauth=${testUserToken}"                           | "token"
    }

    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables()

    @Unroll
    @Ignore
    def "can set authentication in environment with #authMethod"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "changing home variable"
        System.setProperty("user.home", projectDir.path)

        and: "task with errors"
        assert runTasksWithFailure("testPublish")

        fork = true
        when: "setting auth properties in environment"

        if (username) {
            environmentVariables.set("GITHUB_LOGIN", username)
        }

        if (password) {
            environmentVariables.set("GITHUB_PASSWORD", password)
        }

        if (token) {
            environmentVariables.set("GITHUB_OAUTH", token)
        }

        if (baseUrl) {
            environmentVariables.set("GITHUB_ENDPOINT", baseUrl)
        }

        then:
        runTasksSuccessfully("testPublish")

        where:
        username     | password      | token         | baseUrl                  | authMethod
        //testUserName | testUserToken | null          | null                     | "username/password"
        testUserName | null          | testUserToken | null                     | "username/token"
//        null         | null          | testUserToken | null                     | "token"
        testUserName | testUserToken | null          | "https://api.github.com" | "username/password/baseUrl"
//        testUserName | null          | testUserToken | "https://api.github.com" | "username/token/baseUrl"
//        null         | null          | testUserToken | "https://api.github.com" | "token/baseUrl"
    }

    def "gets client from set up credentials"() {
        given:
        buildFile << """
            github {
                username="${testRepo.userName}"
                token="${testRepo.token}"
            }
            task(custom) {
                doLast {
                    def client = github.clientProvider.get()
                    println("Repository: " + client.getRepository("${testRepo.repository.fullName}").fullName)
                }
            }            
        """
        when:
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("Repository: ${testRepo.repository.fullName}".toString())
    }

}
