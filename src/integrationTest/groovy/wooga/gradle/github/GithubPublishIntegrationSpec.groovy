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

import spock.lang.Shared
import spock.lang.Unroll

class GithubPublishIntegrationSpec extends GithubPublishIntegration {

    def "task gets skipped when source is empty"() {
        given: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
            }
        """

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        result.standardOutput.contains("testPublish NO-SOURCE")
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
                tagName = "test"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease("test")
        def assets = release.assets
        assets.size() == 2
        assets.any { it.name == "fileOne" }
        assets.any { it.name == "fileTwo" }
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
                tagName = "test"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease("test")
        def assets = release.assets
        assets.size() == 2
        assets.any { it.name == "one.zip" }
        assets.any { it.name == "two.zip" }
    }

    @Unroll("fails when calling unsurported Copy API #api")
    def "fails when using unsurported Copy API"() {
        given: "some test files to publish"
        File sources = new File(projectDir, "sources")
        sources.mkdirs()
        createFile("fileOne", sources)
        createFile("fileTwo", sources)

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "sources"
                into "buildDir"
            }
        """
        expect:
        def result = runTasksWithFailure("testPublish")
        result.standardError.contains("method not supported")


        where:
        api                                                                           | _
        'into "buildDir"'                                                             | _
        'into("buildDir"){}'                                                          | _
        'destinationDir file("buildDir")'                                             | _
        "into(new Action<CopySpec> { @Override void execute(CopySpec copySpec) {} })" | _
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
                tagName = "v0.1.0"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease("v0.1.0")
        def assets = release.assets
        assets.size() == 1
        assets.any { it.name == "fileToPublish.json" }
    }


}
