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

class GithubPublishIntegrationSpec extends GithubPublishIntegrationWithDefaultAuth {

    def "task creates just the release when asset source is empty"() {
        given: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.assets
        assets.size() == 0

        where:
        tagName = "v0.1.0-GithubPublishIntegrationSpec"
    }

    @Unroll
    def "can use PatternFilterable API to configure task #method #filter"() {
        given: "some test files to publish"
        File sources = new File(projectDir, "sources")
        sources.mkdirs()

        def file1 = createFile("fileOne", sources)
        file1 << """test"""

        def file2 = createFile("fileTwo", sources)
        file2 << """YO"""

        def file3 = createFile("fileThree", sources)
        file3 << """YO"""

        def file4 = createFile("fileFour", sources)
        file4 << """YO"""

        def file5 = createFile("fileFive", sources)
        file5 << """YO"""

        def file6 = createFile("fileSix", sources)
        file6 << """YO"""

        def file7 = createFile("fileSeven", sources)
        file7 << """YO"""

        def file8 = createFile("fileEight", sources)
        file8 << """YO"""

        def file9 = createFile("fileNine", sources)
        file9 << """YO"""

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "sources"
                ${method}($filter)             
                tagName = "$tagName"

                println(getExcludes())
                println(getIncludes())
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.assets
        assets.size() == 1
        assets.any { it.name == "fileNine" }

        where:
        method    | filter
        "exclude" | "'*One', '*T*', '*S*', '*F*', '*E*'"
        "exclude" | "{it.file in fileTree(dir:'sources', excludes:['*Nine']).files}"
        "exclude" | "['*One', '*T*', '*S*', '*F*', '*E*']"
        "include" | "'*Nine'"
        "include" | "{it.file in fileTree(dir:'sources', excludes:['*One', '*T*', '*S*', '*F*', '*E*']).files}"
        "include" | "['*Nine']"

        tagName = "v0.1.1-GithubPublishIntegrationSpec"
    }

    def "can use CopySourceSpec API to configure task"() {
        given: "some test files to publish"
        File sources = new File(projectDir, "sources")
        sources.mkdirs()
        def file1 = createFile("fileOne", sources)
        file1 << """test"""
        def file2 = createFile("fileTwo", sources)
        file2 << """YO"""

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "sources"
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        !release.isDraft()
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
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
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
        !release.isDraft()
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
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "fileToPublish"
                repositoryName = "${testUserName}/customRepo"
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
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
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
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.assets
        assets.size() == 1
        assets.any { it.name == "fileToPublish.json" }

        where:
        tagName = "v0.3.0-GithubPublishIntegrationSpec"
    }
}
