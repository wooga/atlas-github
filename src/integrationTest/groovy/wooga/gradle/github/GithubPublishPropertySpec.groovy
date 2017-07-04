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

class GithubPublishPropertySpec extends GithubPublishIntegration {

    def "task skips if repository is not set"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "releaseAssets"
                tagName = "v0.1.0"
            }
        """

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        result.wasSkipped("testPublish")
    }
}
