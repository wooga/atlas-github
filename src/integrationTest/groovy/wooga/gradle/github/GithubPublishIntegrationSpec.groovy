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

import nebula.test.IntegrationSpec
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import spock.lang.Shared
import wooga.gradle.github.base.GithubBasePlugin

class GithubPublishIntegrationSpec extends IntegrationSpec {

    String uniquePostfix() {
        String key = "TRAVIS_JOB_NUMBER"
        def env = System.getenv()
        if(env.containsKey(key)) {
            return env.get(key)
        }
        return ""
    }

    @Shared
    def availableRepo = "atlas-github-integration" + uniquePostfix()

    @Shared
    def availableOwner = System.getenv("ATLAS_GITHUB_INTEGRATION_USER")

    @Shared
    def availableToken = System.getenv("ATLAS_GITHUB_INTEGRATION_PASSWORD")

    @Shared
    GitHub client

    @Shared
    GHRepository testRepo

    def createTestRepo() {

        try {
            def repository = client.getRepository("$availableOwner/$availableRepo")
            repository.delete()
        }
        catch (Exception e) {

        }


        def builder = client.createRepository(availableRepo)
        builder.description("Integration test repo for wooga/atlas-github")
        builder.autoInit(false)
        builder.licenseTemplate('MIT')
        builder.private_(false)
        builder.issues(false)
        builder.wiki(false)
        testRepo = builder.create()
    }

    def createRelease(String tagName) {
        def builder = testRepo.createRelease(tagName)
        builder.create()
    }

    def setupSpec() {
        client = GitHub.connectUsingOAuth(availableToken)
        createTestRepo()
    }

    def setup() {
        buildFile << """
            ${applyPlugin(GithubPlugin)}

            github {
                owner = "$availableOwner"
                repository = "$availableRepo"
                token = "$availableToken"
            }
        """.stripIndent()
    }

    def cleanup() {
        def releases = testRepo.listReleases()
        releases.each {
            it.delete()
        }
    }

    def cleanupSpec() {
        testRepo.delete()
    }

    GHRelease getRelease(String tagName) {
        (GHRelease) testRepo.listReleases().find({it.tagName == tagName})
    }


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
        assets.any {it.name == "fileOne"}
        assets.any {it.name == "fileTwo"}
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
        assets.any {it.name == "one.zip"}
        assets.any {it.name == "two.zip"}
    }

    def "fails when using into"() {
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
        runTasksWithFailure("testPublish")
    }

    def "fails when repo is not available"() {
        given: "a file to publish"
        def file = createFile("fileToPublish")
        file << """YUP"""

        and: "a buildfile with publish task and non existing repo"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "fileToPublish"
                repository = "customRepo"
                tagName = "test"
            }
        """

        expect:
        def result = runTasksWithFailure("testPublish")
        result.standardError.contains("can't find repository $availableOwner/customRepo")
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
        assets.any {it.name == "fileToPublish.json"}
    }
}
