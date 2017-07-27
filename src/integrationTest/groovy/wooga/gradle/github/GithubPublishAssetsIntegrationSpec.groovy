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
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
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
        def assets = release.assets
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

            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from configurations.githubAssets
                tagName = "$tagName"
            }
        """

        when:
        runTasksSuccessfully("testPublish")

        then:
        def release = getRelease(tagName)
        def assets = release.assets
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
        assets.size() == 2
        assets.any { it.name == "package.zip" }
        assets.any { it.name == "fileToPublish.json" }

        where:
        tagName = "v0.4.0-GithubPublishAssetsIntegrationSpec"
    }
}
