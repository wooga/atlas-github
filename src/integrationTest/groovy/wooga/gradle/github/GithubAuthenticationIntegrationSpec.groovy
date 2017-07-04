/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import spock.lang.Unroll

class GithubAuthenticationIntegrationSpec extends GithubPublishIntegration {

    def setup() {
        buildFile << """
            github.repository = "$testRepositoryName"

            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "releaseAssets"
                tagName = "v0.1.0"
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
        assert results.standardError.contains("Bad credentials")

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
        "userName = '${testUserName}'; password = '${testUserToken}'"                        | "username/password"
        "userName = '${testUserName}'; token = '${testUserToken}'"                           | "username/token"
        "token = '${testUserToken}'"                                                         | "token"
        "token('${testUserToken}').userName('${testUserName}').password('${testUserToken}')" | "username/password/token set with chained method"
    }

    @Unroll
    def "can set authentication in gradle properties with #authMethod"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "task with errors"
        assert runTasksWithFailure("testPublish")

        when: "setting auth properties"
        createFile("gradle.properties") << accessParams

        then:
        runTasksSuccessfully("testPublish")

        where:
        accessParams                                                        | authMethod
        "github.userName=${testUserName}\ngithub.password=${testUserToken}" | "username/password"
        "github.userName=${testUserName}\ngithub.token=${testUserToken}"    | "username/token"
        "github.token=${testUserToken}"                                     | "token"
    }

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties()

    @Unroll
    def "can set authentication in ~/.github with #authMethod"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

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
    def "can set authentication in environment with #authMethod"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "task with errors"
        assert runTasksWithFailure("testPublish")

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
        testUserName | testUserToken | null          | null                     | "username/password"
        testUserName | null          | testUserToken | null                     | "username/token"
        null         | null          | testUserToken | null                     | "token"
        testUserName | testUserToken | null          | "https://api.github.com" | "username/password/baseUrl"
        testUserName | null          | testUserToken | "https://api.github.com" | "username/token/baseUrl"
        null         | null          | testUserToken | "https://api.github.com" | "token/baseUrl"
    }
}
