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
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        result.wasSkipped("testPublish")
    }

    @Unroll
    def "can set #method with #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "0.1.0"

            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "releaseAssets"
                tagName = "v0.1.0"
                repository = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()

        if (isDraftValue) {
            buildFile << """
            testPublish.$methodName($isDraftValue)
            """.stripIndent()
        }

        if (isPrereleaseValue) {
            buildFile << """
            testPublish.$methodName($isPrereleaseValue)
            """.stripIndent()
        }

        if (bodyValue) {
            buildFile << """
            testPublish.$methodName("$bodyValue")
            """.stripIndent()
        }

        if (releaseNameValue) {
            buildFile << """
            testPublish.$methodName("$releaseNameValue")
            """.stripIndent()
        }

        if (targetCommitishValue) {
            buildFile << """
            testPublish.$methodName("$targetCommitishValue")
            """.stripIndent()
        }

        when:
        runTasksSuccessfully("testPublish")

        then:
        def releaseValueCheck = releaseNameValue ? releaseNameValue : "0.1.0"
        def targetCommitishValueCheck = targetCommitishValue ? targetCommitishValue : "master"

        hasReleaseByName(releaseValueCheck)
        def release = getReleaseByName(releaseValueCheck)

        release.isDraft() == isDraftValue
        release.isPrerelease() == isPrereleaseValue
        release.getBody() == bodyValue
        release.getName() == releaseValueCheck
        release.getTargetCommitish() == targetCommitishValueCheck

        where:
        method            | isDraftValue | isPrereleaseValue | bodyValue  | releaseNameValue      | targetCommitishValue               | useSetter
        "draft"           | true         | false             | null       | null                  | null                               | true
        "draft"           | true         | false             | null       | null                  | null                               | false
        "prerelease"      | false        | true              | null       | null                  | null                               | true
        "prerelease"      | false        | true              | null       | null                  | null                               | false
        "body"            | false        | false             | "testBody" | null                  | null                               | true
        "body"            | false        | false             | "testBody" | null                  | null                               | false
        "releaseName"     | false        | false             | null       | "testPropertyRelease" | null                               | true
        "releaseName"     | false        | false             | null       | "testPropertyRelease" | null                               | false
        "targetCommitish" | false        | false             | null       | null                  | testRepo.listCommits().last().SHA1 | true
        "targetCommitish" | false        | false             | null       | null                  | testRepo.listCommits().last().SHA1 | false

        methodName = useSetter ? "set${method.capitalize()}" : method
    }

    @Unroll
    def "can set tagName with #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "0.1.0"

            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "releaseAssets"
                $methodName("$tagNameValue")
                draft = false
                repository = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()

        then:
        when:
        runTasksSuccessfully("testPublish")

        then:
        hasRelease(tagNameValue)

        where:
        tagNameValue        | useSetter
        "testReleaseTagOne" | true
        "testReleaseTagTwo" | false

        methodName = useSetter ? "setTagName" : "tagName"
    }

    @Unroll
    def "fails when setting #methodName with invalid repository name #repoName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "0.1.0"

            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "releaseAssets"
                tagName = "v0.1.0"
                $methodName($repoName)
                token = "$testUserToken"
            }            
        """.stripIndent()

        expect:
        def result = runTasksWithFailure("testPublish")
        result.standardError.contains(expectedError)

        where:
        repoName                                 | useSetter | expectedError
        "'invalid'"                              | true      | "Repository value $repoName is not a valid github repository name"
        "'invalid'"                              | false     | "Repository value $repoName is not a valid github repository name"
        null                                     | true      | "java.lang.IllegalArgumentException: repository"
        null                                     | false     | "java.lang.IllegalArgumentException: repository"
        "''"                                     | false     | "java.lang.IllegalArgumentException: repository"
        "''"                                     | false     | "java.lang.IllegalArgumentException: repository"
        "'https://github.com/owner/invalid.git'" | true      | "Repository value $repoName is not a valid github repository name"
        "'https://github.com/owner/invalid.git'" | false     | "Repository value $repoName is not a valid github repository name"

        methodName = useSetter ? "setRepository" : "repository"
    }

    @Unroll
    def "fails when setting #methodName with invalid token #token"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "0.1.0"

            task testPublish(type:wooga.gradle.github.publish.GithubPublish) {
                from "releaseAssets"
                tagName = "v0.1.0"
                $methodName($token)
                repository = "$testRepositoryName"
                
            }            
        """.stripIndent()

        expect:
        def result = runTasksWithFailure("testPublish")
        result.standardError.contains("java.lang.IllegalArgumentException: token")

        where:
        token | useSetter
        "''"  | true
        "''"  | false
        null  | true
        null  | false

        methodName = useSetter ? "setToken" : "token"
    }
}
