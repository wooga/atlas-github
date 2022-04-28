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


import spock.lang.Ignore
import spock.lang.Unroll

@Ignore
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
        sleep(2000)
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.listAssets()
        assets.size() == 0

        where:
        tagName = "v0.1.0-GithubPublishIntegrationSpec"
    }

    @Ignore
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
        sleep(1000)
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.listAssets()
        assets.size() == 1
        assets.any { it.name == "fileNine" }

        where:
        method    | filter                                                                                      | tagVersion
        "exclude" | "'*One', '*T*', '*S*', '*F*', '*E*'"                                                        | 1
        "exclude" | "{it.file in fileTree(dir:'sources', excludes:['*Nine']).files}"                            | 2
        "exclude" | "['*One', '*T*', '*S*', '*F*', '*E*']"                                                      | 3
        "include" | "'*Nine'"                                                                                   | 4
        "include" | "{it.file in fileTree(dir:'sources', excludes:['*One', '*T*', '*S*', '*F*', '*E*']).files}" | 5
        "include" | "['*Nine']"                                                                                 | 6

        tagName = "v0.1.${tagVersion}-GithubPublishIntegrationSpec"
    }

    @Ignore
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
        sleep(1000)
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.listAssets()
        assets.size() == 2
        assets.any { it.name == "fileOne" }
        assets.any { it.name == "fileTwo" }

        where:
        tagName = "v0.8.0-GithubPublishIntegrationSpec"
    }

    @Ignore
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
        def assets = release.listAssets()
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
        outputContains(result, "can't find repository $testUserName/customRepo")
    }

    def "fails when release already exists and publishMethod is create"() {
        given: "a release with tagname"
        def tagName = "testTag"
        createRelease(tagName)
        sleep(1000)

        and: "a file to publish"
        createFile("fileToPublish")

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "fileToPublish"
                tagName = "$tagName"
                publishMethod = "create" //that is also the default value
            }
        """

        expect:
        def result = runTasksWithFailure("testPublish")
        outputContains(result, "github release with tag ${tagName} already exist")
    }

    def "fails when release doesn't exists and publishMethod is update"() {
        given: "a tagname"
        def tagName = "testTag"

        and: "a file to publish"
        createFile("fileToPublish")

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "fileToPublish"
                tagName = "$tagName"
                publishMethod = "update"
            }
        """

        expect:
        def result = runTasksWithFailure("testPublish")
        outputContains(result, "github release with tag ${tagName} for update not found")
    }

    @Unroll
    @Ignore
    def "#messages and publishMethod is createOrUpdate"() {
        given: "an optional release"
        if (releaseAvailable) {
            def builder = testRepo.createRelease(tagName)
            builder.name("Initial Release")
            builder.create()
        }

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                tagName = "$tagName"
                releaseName = "$tagName" 
                publishMethod = "createOrUpdate"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        sleep(1000)
        def release = getRelease(tagName)
        release.name == tagName

        where:
        releaseAvailable | tagName                               | messages
        true             | "v0.5.0-GithubPublishIntegrationSpec" | "updates release when release exists"
        false            | "v0.6.0-GithubPublishIntegrationSpec" | "creates release when release doesn't exists"
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
        sleep(1000)
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.listAssets()
        assets.size() == 1
        assets.any { it.name == "fileToPublish.json" }

        where:
        tagName = "v0.3.0-GithubPublishIntegrationSpec"
    }

    @Ignore
    def "updates a release when publishMethod is update"() {
        given: "multiple files to publish"
        def fromDirectory = new File(projectDir, "initialReleaseAssets")
        fromDirectory.mkdirs()
        createFile("initial.json", fromDirectory) << """{"body" : "initial"}"""

        fromDirectory = new File(projectDir, "updateReleaseAssets")
        fromDirectory.mkdirs()
        createFile("update.json", fromDirectory) << """{"body" : "update"}"""
        createFile("initial.json", fromDirectory) << """{"body" : "update"}"""

        and: "a buildfile with publish task"
        buildFile << """
            task testPublishOrUpdate(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                tagName = "$tagName"
                publishMethod = "update"
                
                releaseName = "${expectedName}"
                body = "${expectedBody}"
                prerelease = ${expectedPrerelease}
                draft = ${expectedDraft}

                from "updateReleaseAssets"
            }

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                tagName = "$tagName"
                
                releaseName = "${initialName}"
                body = "${initialBody}"
                prerelease = ${initialPrerelease}
                draft = ${initialDraft}

                from "initialReleaseAssets"
            }
        """

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        release.getName() == initialName
        release.getBody() == initialBody
        release.isPrerelease() == initialPrerelease
        release.isDraft() == initialDraft

        def assets = release.listAssets()
        assets.size() == 1
        assets.any { it.name == "initial.json" }

        when:
        result = runTasksSuccessfully("testPublishOrUpdate")

        then:
        def updatedRelease = getRelease(tagName)
        updatedRelease.getName() == expectedName
        updatedRelease.getBody() == expectedBody
        updatedRelease.isPrerelease() == expectedPrerelease
        updatedRelease.isDraft() == expectedDraft
        updatedRelease.targetCommitish == expectedTargetCommitish

        def updatedAssets = updatedRelease.listAssets()
        updatedAssets.size() == 2
        updatedAssets.any { it.name == "update.json" }
        updatedAssets.any { it.name == "initial.json" }
        //Skip detailed asset content check. There seems to be an issue with github
        //updatedAssets.any { new URL(it.browserDownloadUrl).text == """{"body" : "update"}""" }

        where:
        expectedName = "Updated Release Name"
        expectedBody = "Updated Body"
        expectedPrerelease = false
        expectedDraft = false
        expectedTargetCommitish = testRepo.defaultBranch.name

        initialName = expectedName.replace("Update", "Initial")
        initialBody = expectedBody.replace("Update", "Initial")
        initialPrerelease = !expectedPrerelease
        initialDraft = !expectedDraft

        tagName = "v0.4.0-GithubPublishIntegrationSpec"
    }

    def "cleans up new created releases on failure"() {
        given: "an empty file to publish (this will fail)"
        def fromDirectory = new File(projectDir, "initialReleaseAssets")
        fromDirectory.mkdirs()
        createFile("initial.json", fromDirectory)

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                tagName = "$tagName"
                from "initialReleaseAssets"
            }
        """

        when:
        runTasksWithFailure("testPublish")

        then:
        !getRelease(tagName)

        where:
        tagName = "v0.7.0-GithubPublishIntegrationSpec"

    }
}
