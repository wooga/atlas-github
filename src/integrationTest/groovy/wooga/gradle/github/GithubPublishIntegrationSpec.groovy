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

class GithubPublishIntegrationSpec extends GithubPublishIntegrationWithDefaultAuth {

    def "task creates just the release when asset source is empty"() {
        given: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        def assets = release.assets
        assets.size() == 0

        where:
        tagName = "v0.1.0-GithubPublishIntegrationSpec"
    }

    def "use copy spec for GithubPlublish task configuration"() {
        given: "some test files to publish"
        File sources = new File(projectDir, "sources")
        sources.mkdirs()
        def file1 = createFile("fileOne", sources)
        file1 << """test"""
        def file2 = createFile("fileTwo", sources)
        file2 << """YO"""

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "sources"
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        def assets = release.assets
        assets.size() == 2
        assets.any { it.name == "fileOne" }
        assets.any { it.name == "fileTwo" }

        where:
        tagName = "v0.1.0-GithubPublishIntegrationSpec"
    }

    def "can nest export directory"() {
        given: "some test files to publish"
        File sources = new File(projectDir, "sources")
        sources.mkdirs()
        File sources2 = new File(projectDir, "sources2")
        sources2.mkdirs()
        createFile("fileOne", sources)
        createFile("fileTwo", sources2)

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from(project.file("sources")) {
                    into "one"
                }

                from(project.file("sources2")) {
                    into "two"
                }
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        def assets = release.assets
        assets.size() == 2
        assets.any { it.name == "one.zip" }
        assets.any { it.name == "two.zip" }

        where:
        tagName = "v0.2.0-GithubPublishIntegrationSpec"
    }

    def "fails when repo is not available"() {
        given: "a file to publish"
        def file = createFile("fileToPublish")
        file << """YUP"""

        and: "a buildfile with publish task and non existing repo"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "fileToPublish"
                repository = "${testUserName}/customRepo"
                tagName = "test"
            }
        """

        expect:
        def result = runTasksWithFailure("testPublish")
        result.standardError.contains("can't find repository $testUserName/customRepo")
    }

    def "fails when release already exists"() {
        given: "a release with tagname"
        def tagName = "testTag"
        createRelease(tagName)

        and: "a file to publish"
        createFile("fileToPublish")

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "fileToPublish"
                tagName = "$tagName"
            }
        """

        expect:
        def result = runTasksWithFailure("testPublish")
        result.standardError.contains("github release with tag ${tagName} already exist")
    }

    def "publish release with assets"() {
        given: "a file to publish"
        def fromDirectory = new File(projectDir, "releaseAssets")
        fromDirectory.mkdirs()
        def file = createFile("fileToPublish.json", fromDirectory)
        file << """
        {
            "body" : "awesome" 
        }
        """.stripIndent()

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "releaseAssets"
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        def assets = release.assets
        assets.size() == 1
        assets.any { it.name == "fileToPublish.json" }

        where:
        tagName = "v0.3.0-GithubPublishIntegrationSpec"
    }
}
