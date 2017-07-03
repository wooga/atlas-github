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

import spock.lang.Unroll

class GithubAuthenticationIntegrationSpec extends GithubPublishIntegration {

    @Unroll
    def "can override authentication parameters in GithubPublish task with #accessParams"() {
        given: "files to publish"
        def assetsFiles = createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "releaseAssets"
                tagName = "v0.1.0"
            }
        """.stripIndent()

        and: "task runs without errors"
        assert runTasksSuccessfully("testPublish")

        when: "overriding github access parameters"
        cleanupReleases()
        buildFile << """
            testPublish {
                $accessParams
            }
        """.stripIndent()

        then:
        def result = runTasksWithFailure("testPublish")
        result.standardError.contains("Bad credentials")

        where:
        accessParams                                      | _
        'userName = "test";password = "test"'             | _
        'password = "test"'                               | _
        'token = "test"'                                  | _
        'token("test").userName("test").password("test")' | _
    }
}
