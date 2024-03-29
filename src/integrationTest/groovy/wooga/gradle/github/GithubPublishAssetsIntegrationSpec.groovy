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

import spock.genesis.Gen
import spock.genesis.transform.Iterations
import spock.lang.*

@Ignore
class GithubPublishAssetsIntegrationSpec extends GithubPublishIntegrationWithDefaultAuth {

    def "publish directories as zip archives"() {
        given: "a directory with files"
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
                from("releaseAssets") {
                    into "package"
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
        assets.size() == 1
        assets.any { it.name == "package.zip" }

        where:
        tagName = "v0.1.0-GithubPublishAssetsIntegrationSpec"
    }

    def "publish directories in directories as zip archives"() {
        given: "a directory with files"
        def fromDirectory = new File(projectDir, "releaseAssets")
        fromDirectory.mkdirs()

        def aDirectoryWithFiles = new File(fromDirectory, "package")
        aDirectoryWithFiles.mkdirs()
        def file = createFile("fileToPublish.json", aDirectoryWithFiles)
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
        def assets = release.listAssets()
        assets.size() == 1
        assets.any { it.name == "package.zip" }

        where:
        tagName = "v0.2.0-GithubPublishAssetsIntegrationSpec"
    }

    def "publish files from configurations"() {
        given: "a few files"

        createFile("fileToPublish1.json") << "test content"
        createFile("fileToPublish2.json") << "test content"
        createFile("fileToPublish3.json") << "test content"
        createFile("fileToPublish4.json") << "test content"

        and: "a buildfile with publish task and custom configuration"
        buildFile << """
            configurations {
                githubAssets
            }
            
            dependencies {
                githubAssets files("fileToPublish1.json")
                githubAssets files("fileToPublish2.json")
                githubAssets files("fileToPublish3.json")
                githubAssets files("fileToPublish4.json")
            }

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from configurations.githubAssets
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.listAssets()
        assets.size() == 4
        assets.every() { it.name =~ /fileToPublish\d\.json/ }

        where:
        tagName = "v0.3.0-GithubPublishAssetsIntegrationSpec"
    }

    def "publish files and directories"() {
        given: "a directory with files"
        def fromDirectory = new File(projectDir, "releaseAssets")
        fromDirectory.mkdirs()

        def aDirectoryWithFiles = new File(fromDirectory, "package")
        aDirectoryWithFiles.mkdirs()
        def file = createFile("fileToPublish.json", aDirectoryWithFiles)
        file << """
        {
            "body" : "awesome" 
        }
        """.stripIndent()

        def file2 = createFile("fileToPublish.json", fromDirectory)
        file2 << """
        {
            "body" : "awesome two" 
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
        def assets = release.listAssets()
        assets.size() == 2
        assets.any { it.name == "package.zip" }
        assets.any { it.name == "fileToPublish.json" }

        where:
        tagName = "v0.4.0-GithubPublishAssetsIntegrationSpec"
    }

    @Unroll
    def "publish files with special characters [#fileName]"() {
        given: "a directory with files"
        def fromDirectory = new File(projectDir, "releaseAssets")
        fromDirectory.mkdirs()
        def file = createFile(fileName, fromDirectory)
        file << """
        {
            "body" : "awesome"
        }
        """.stripIndent()

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from("releaseAssets")
                tagName = "$tagName"
            }
        """

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        !release.isDraft()
        def assets = release.listAssets()
        assets.size() == 1
        assets.any { it.name == expectedFileName }
        outputContains(result, "asset '${fileName}' renamed by github to '${expectedFileName}'")

        where:
        fileName                   | expectedFileName           | tag
        "file to publish.json"     | "file+to+publish.json"     | "0.5.0"
        "filetoöÖäÄüÜpublish.json" | "fileto.C3.B6.C3.96.C3.A4.C3.84.C3.BC.C3.9Cpublish.json" | "0.6.0"

        tagName = "v${tag}-GithubPublishAssetsIntegrationSpec"
    }

    def "removes added assets when release update fails"() {
        given: "a release"
        createRelease(tagName)

        and: "a directory with release assets"
        def fromDirectory = new File(projectDir, "releaseAssets")
        fromDirectory.mkdirs()

        and: "a faulty asset"
        createFile("working3.json", fromDirectory)

        and: "a working asset"
        def workingAsset0 = createFile("working0.json", fromDirectory) << """{"body" : "awesome"}"""

        and: "a working asset"
        def workingAsset1 = createFile("working1.json", fromDirectory) << """{"body" : "awesome"}"""


        and: "a working asset"
        def workingAsset2 = createFile("working2.json", fromDirectory) << """{"body" : "awesome"}"""


        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from("releaseAssets")
                tagName = "$tagName"
                publishMethod = "update"
            }
        """

        when:
        def result = runTasksWithFailure("testPublish")

        then:
        def release = getRelease(tagName)

        def assets = release.listAssets()
        assets.size() == 0

        outputContains(result, "delete published asset ${workingAsset0.name}")
        outputContains(result, "delete published asset ${workingAsset1.name}")
        outputContains(result, "delete published asset ${workingAsset2.name}")

        where:
        tagName = "v0.7.0-GithubPublishAssetsIntegrationSpec"
    }

    def "keeps old assets when release update fails"() {
        given: "a release"
        def presentRelease = createRelease(tagName)

        and: "an published asset"
        def publishedAsset = createFile("published.json") << """{"body" : "awesome"}"""
        presentRelease.uploadAsset(publishedAsset, "text/plain")

        and: "a directory with release assets"
        def fromDirectory = new File(projectDir, "releaseAssets")
        fromDirectory.mkdirs()

        and: "a faulty asset"
        createFile("working3.json", fromDirectory)

        and: "a working asset"
        def workingAsset1 = createFile("working1.json", fromDirectory) << """{"body" : "awesome"}"""


        and: "a working asset"
        def workingAsset2 = createFile("working2.json", fromDirectory) << """{"body" : "awesome"}"""


        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from("releaseAssets")
                tagName = "$tagName"
                publishMethod = "update"
            }
        """

        when:
        def result = runTasksWithFailure("testPublish")

        then:
        def release = getRelease(tagName)

        def assets = release.listAssets()
        assets.size() == 1
        assets.getAt(0).name == publishedAsset.name

        outputContains(result, "delete published asset ${workingAsset1.name}")
        outputContains(result, "delete published asset ${workingAsset2.name}")

        where:
        tagName = "v0.8.0-GithubPublishAssetsIntegrationSpec"
    }

    def "restores content of updated assets when release update fails"() {
        given: "a release"
        def presentRelease = createRelease(tagName)

        and: "an published asset"
        def publishedAsset = createFile("published.json") << """{"body" : "initial"}"""
        presentRelease.uploadAsset(publishedAsset, "text/plain")

        and: "a directory with release assets"
        def fromDirectory = new File(projectDir, "releaseAssets")
        fromDirectory.mkdirs()

        and: "a faulty asset"
        createFile("working3.json", fromDirectory)

        and: "a working asset"
        def updatedAsset1 = createFile("published.json", fromDirectory) << """{"body" : "updated"}"""


        and: "a working asset"
        def workingAsset2 = createFile("working2.json", fromDirectory) << """{"body" : "awesome"}"""


        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from("releaseAssets")
                tagName = "$tagName"
                publishMethod = "update"
            }
        """

        when:
        def result = runTasksWithFailure("testPublish")

        then:
        def release = getRelease(tagName)

        def assets = release.listAssets()
        assets.size() == 1
        assets.getAt(0).name == publishedAsset.name

        //It seems there is an issue when accessing deleted assets.
        //The redirection points to the asset file we just deleted
        //new URL(assets.get(0).browserDownloadUrl).text == """{"body" : "initial"}"""

        outputContains(result, "restore updated asset ${updatedAsset1.name}")
        outputContains(result, "delete published asset ${workingAsset2.name}")
        outputContains(result, "delete published asset ${updatedAsset1.name}")

        where:
        tagName = "v0.9.0-GithubPublishAssetsIntegrationSpec"
    }

    @Shared
    def characterPattern = ':_\\-<>|*\\\\? üäö¨áóéçΩå´´£¢∞§¶'

    @IgnoreIf({ os.windows })
    @Iterations(20)
    @Unroll
    def "publish files with special characters (#fileName)"() {
        given: "a directory with files"
        def fromDirectory = new File(projectDir, "releaseAssets")
        fromDirectory.mkdirs()
        def file = createFile(fileName, fromDirectory)
        file << """
        {
            "body" : "awesome"
        }
        """.stripIndent()

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from("releaseAssets")
                tagName = "$tagName"
            }
        """

        expect:
        runTasksSuccessfully("testPublish")

        where:
        tag << Gen.integer(20, Integer.MAX_VALUE)
        fileName << Gen.string(~/file([${characterPattern}]{10,20})\.json/)
        tagName = "v0.${tag}.0-GithubPublishAssetsIntegrationSpec"

    }

    @Issue("https://github.com/wooga/atlas-github/issues/30")
    def "executes depended tasks and fetches task outputs"() {
        given: "a task that produces an output"
        buildFile << """
        task createOutput {
            outputs.file("build/outputs/${assetName}")
            
            doLast {
                def outputFile = file("build/outputs/${assetName}")
                outputFile.parentFile.mkdirs()
                outputFile.text = "${assetContent}"
            }
        }
        """.stripIndent()

        and: "a publish task which from points to the custom task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                tagName = "$tagName"
                releaseName = "$tagName" 
                
                from(createOutput)
            }
        """

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        result.wasExecuted("createOutput")
        def release = getRelease(tagName)
        def assets = release.listAssets()
        assets.size() == 1
        assets[0].name == assetName

        where:
        tagName = "v0.8.0-GithubPublishIntegrationSpec"
        assetName = "list.txt"
        assetContent = "Custom content"
    }

    @Issue("https://github.com/wooga/atlas-github/issues/30")
    def "executes depended tasks through configuration"() {
        given: "a subproject that produces an artifact"
        addSubproject("sub", """
        plugins {
            id "base"
        }

        def outputFile = file("build/outputs/${assetName}")
        
        task createOutput {            
            doLast {
                outputFile.parentFile.mkdirs()
                outputFile.text = "${assetContent}"
            }
        }
        
        assemble.dependsOn createOutput
        
        configurations {
            output
        }
        
        configurations['default'].extendsFrom(configurations.output)
        
        artifacts {
            output(outputFile) {
                builtBy project.tasks.createOutput
            }
        }

        """.stripIndent())

        and: "a dependency to the sub project artifact"
        buildFile << """
        
        configurations {
            sub
        }
        
        dependencies {
            sub project(':sub')
        }

        """.stripIndent()

        and: "a publish task which from points to a configuration"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                tagName = "$tagName"
                releaseName = "$tagName" 
                
                from(configurations.sub)
            }
        """

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        result.wasExecuted(":sub:createOutput")
        def release = getRelease(tagName)
        def assets = release.listAssets()
        assets.size() == 1
        assets[0].name == assetName

        where:
        tagName = "v0.9.0-GithubPublishIntegrationSpec"
        assetName = "output.txt"
        assetContent = "Custom content"

    }

    def "publish assets from sub projects"() {
        given:
        String javaProject = """
        plugins {
            id "java"
        }
        """.stripIndent()

        File sub1 = addSubproject("lib1", javaProject)
        File sub2 = addSubproject("lib2", javaProject)

        writeHelloWorld(sub1)
        writeHelloWorld(sub2)

        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from {subprojects.configurations.archives.artifacts.files}
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
        assets.any { it.name == "lib1.jar" }
        assets.any { it.name == "lib2.jar" }

        where:
        tagName = "v0.1.1-MultiProjectPublishIntegrationSpec"
    }
}
