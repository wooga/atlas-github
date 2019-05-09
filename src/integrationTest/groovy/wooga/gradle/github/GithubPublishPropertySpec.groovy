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

import org.kohsuke.github.GHRepository
import spock.lang.Issue
import spock.lang.Retry
import spock.lang.Unroll
import wooga.gradle.github.publish.PublishBodyStrategy
import wooga.gradle.github.publish.PublishMethod

@Retry(mode = Retry.Mode.SETUP_FEATURE_CLEANUP)
class GithubPublishPropertySpec extends GithubPublishIntegration {

    def "task skips if repository is not set"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
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
    def "can set #method with #methodValue and #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "$tagName"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()

        if (isDraftValue) {
            buildFile << """
            testPublish.$methodName($preValue $isDraftValue $postValue)
            """.stripIndent()
        }

        if (isPrereleaseValue) {
            buildFile << """
            testPublish.$methodName($preValue $isPrereleaseValue $postValue)
            """.stripIndent()
        }

        if (bodyValue) {
            buildFile << """
            testPublish.$methodName($preValue "$bodyValue" $postValue)
            """.stripIndent()
        }

        if (releaseNameValue) {
            buildFile << """
            testPublish.$methodName($preValue "$releaseNameValue" $postValue)
            """.stripIndent()
        }

        if (targetCommitishValue) {
            buildFile << """
            testPublish.$methodName($preValue "$targetCommitishValue" $postValue)
            """.stripIndent()
        }

        if (method == "tagName") {
            buildFile << """
            testPublish.$methodName($preValue "$tagName" $postValue)
            """.stripIndent()
        }

        when:
        runTasksSuccessfully("testPublish")

        then:
        def releaseValueCheck = releaseNameValue ? releaseNameValue : versionName
        def targetCommitishValueCheck = targetCommitishValue ? targetCommitishValue : "master"

        hasReleaseByName(releaseValueCheck)
        def release = getReleaseByName(releaseValueCheck)

        release.isDraft() == isDraftValue
        release.isPrerelease() == isPrereleaseValue
        release.getBody() == bodyValue
        release.getName() == releaseValueCheck
        release.getTargetCommitish() == targetCommitishValueCheck

        where:


        method            | isDraftValue | isPrereleaseValue | bodyValue  | releaseNameValue      | targetCommitishValue               | useSetter | isLazy
        "draft"           | true         | false             | null       | null                  | null                               | true      | false
        "draft"           | true         | false             | null       | null                  | null                               | true      | true
        "draft"           | true         | false             | null       | null                  | null                               | false     | false
        "draft"           | true         | false             | null       | null                  | null                               | false     | true
        "prerelease"      | false        | true              | null       | null                  | null                               | true      | false
        "prerelease"      | false        | true              | null       | null                  | null                               | true      | true
        "prerelease"      | false        | true              | null       | null                  | null                               | false     | false
        "prerelease"      | false        | true              | null       | null                  | null                               | false     | true
        "body"            | false        | false             | "testBody" | null                  | null                               | true      | false
        "body"            | false        | false             | "testBody" | null                  | null                               | true      | true
        "body"            | false        | false             | "testBody" | null                  | null                               | false     | false
        "body"            | false        | false             | "testBody" | null                  | null                               | false     | true
        "releaseName"     | false        | false             | null       | "testPropertyRelease" | null                               | true      | false
        "releaseName"     | false        | false             | null       | "testPropertyRelease" | null                               | true      | true
        "releaseName"     | false        | false             | null       | "testPropertyRelease" | null                               | false     | false
        "releaseName"     | false        | false             | null       | "testPropertyRelease" | null                               | false     | true
        "targetCommitish" | false        | false             | null       | null                  | testRepo.listCommits().last().SHA1 | true      | false
        "targetCommitish" | false        | false             | null       | null                  | testRepo.listCommits().last().SHA1 | true      | true
        "targetCommitish" | false        | false             | null       | null                  | testRepo.listCommits().last().SHA1 | false     | false
        "targetCommitish" | false        | false             | null       | null                  | testRepo.listCommits().last().SHA1 | false     | true
        "tagName"         | false        | false             | null       | null                  | null                               | true      | false
        "tagName"         | false        | false             | null       | null                  | null                               | true      | true
        "tagName"         | false        | false             | null       | null                  | null                               | false     | false
        "tagName"         | false        | false             | null       | null                  | null                               | false     | true


        methodName = useSetter ? "set${method.capitalize()}" : method
        methodValue = isLazy ? "closure" : "value"
        preValue = isLazy ? "{" : ""
        postValue = isLazy ? "}" : ""
        tagName = "v0.1.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "can set body with #valueType and #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
        version "$versionName"

        task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
            from "releaseAssets"
            tagName = "$tagName"
            repositoryName = "$testRepositoryName"
            token = "$testUserToken"
        }
        """.stripIndent()

        if (valueType == "File") {
            createFile("test_body.md", projectDir).text = bodyValue

            buildFile << """
            testPublish.$methodName(file("test_body.md"))
            """.stripIndent()
        }

        if (valueType == "Task") {
            buildFile << """

            task generateBody {
                outputs.file("body_from_task.md")
                doLast {
                    file("body_from_task.md").text = "$bodyValue"
                }
            }

            testPublish.$methodName(generateBody)
            """.stripIndent()
        }

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        if (valueType == "Task") {
            result.wasExecuted("generateBody")
        }

        hasReleaseByName(versionName)
        def release = getReleaseByName(versionName)
        release.getBody() == bodyValue

        where:
        bodyValue           | useSetter | valueType
        "test body as file" | false     | "File"
        "test body as file" | true      | "File"
        "test body as task" | false     | "Task"
        "test body as task" | true      | "Task"

        method = "body"
        methodName = useSetter ? "set${method.capitalize()}" : method
        tagName = "v0.1.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "fails to evalute body from task when #reason"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
        version "$versionName"

        task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
            from "releaseAssets"
            tagName = "$tagName"
            repositoryName = "$testRepositoryName"
            token = "$testUserToken"
        }
        """.stripIndent()

        and: "a task with potential body outputs"
        buildFile << """
        task generateBody
        testPublish.body(generateBody)
        """.stripIndent()

        and: "optional outputs"

        (0..<numberOfOutputs).each { i ->
            buildFile << """
            generateBody {
                outputs.file("body_from_task_${i}.md")
                doLast {
                    file("body_from_task_${i}.md").text = "random value"
                }
            }
            """.stripIndent()
        }

        when:
        def result = runTasksWithFailure("testPublish")

        then:
        outputContains(result, expectedError)

        where:
        reason                      | numberOfOutputs | expectedError
        "task has no outputs"       | 0               | "Task provided as body input has no outputs"
        "task has too many outputs" | 2               | "output files to contain exactly one file, however, it contains more than one file"

        tagName = "v0.1.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "can set body with closure and #methodName and closure parameter"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "$tagName"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()


        buildFile << """
            testPublish.$methodName({ repo -> repo.name })
        """.stripIndent()

        when:
        runTasksSuccessfully("testPublish")

        then:
        hasReleaseByName(versionName)
        def release = getReleaseByName(versionName)

        release.getBody() == bodyValue

        where:
        method | bodyValue     | useSetter | isLazy
        "body" | testRepo.name | false     | false
        "body" | testRepo.name | false     | true
        "body" | testRepo.name | true      | false
        "body" | testRepo.name | true      | true

        tagName = "v0.1.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
        methodName = useSetter ? "set${method.capitalize()}" : method
    }

    @Unroll
    def "Set body with closure and #methodName with to many parameters fails"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "$tagName"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()


        buildFile << """
            testPublish.$methodName({ repo, second -> repo.name })
        """.stripIndent()

        when:
        def result = runTasksWithFailure("testPublish")

        then:
        !hasReleaseByName(versionName)
        outputContains(result, "Too many parameters for body clojure")

        where:
        method | bodyValue     | useSetter | isLazy
        "body" | testRepo.name | false     | false
        "body" | testRepo.name | false     | true
        "body" | testRepo.name | true      | false
        "body" | testRepo.name | true      | true

        tagName = "v0.1.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
        methodName = useSetter ? "set${method.capitalize()}" : method

    }

    @Unroll
    def "can set body with object and #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "$tagName"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()


        buildFile << """
            def publishBodyStrategy = [key: "value"]

            testPublish.$methodName(publishBodyStrategy)
        """.stripIndent()

        when:
        runTasksSuccessfully("testPublish")

        then:
        hasReleaseByName(versionName)
        def release = getReleaseByName(versionName)

        release.getBody() == bodyValue.toString()

        where:
        method | bodyValue     | useSetter | isLazy
        "body" | "[key:value]" | false     | false
        "body" | "[key:value]" | false     | true
        "body" | "[key:value]" | true      | false
        "body" | "[key:value]" | true      | true

        tagName = "v0.1.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
        methodName = useSetter ? "set${method.capitalize()}" : method
    }

    @Unroll
    def "can set body with PublishBodyStrategy and #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "$tagName"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()


        buildFile << """
            def publishBodyStrategy = new wooga.gradle.github.publish.PublishBodyStrategy() {
                @Override
                String getBody(org.kohsuke.github.GHRepository ghRepository) {
                    return ghRepository.name
                }
            }

            testPublish.$methodName(publishBodyStrategy)
        """.stripIndent()

        when:
        runTasksSuccessfully("testPublish")

        then:
        hasReleaseByName(versionName)
        def release = getReleaseByName(versionName)

        release.getBody() == bodyValue

        where:
        method | bodyValue     | useSetter | isLazy
        "body" | testRepo.name | false     | false
        "body" | testRepo.name | false     | true
        "body" | testRepo.name | true      | false
        "body" | testRepo.name | true      | true

        publishBodyStrategy = new PublishBodyStrategy() {
            @Override
            String getBody(GHRepository ghRepository) {
                ghRepository.name
            }
        }
        tagName = "v0.1.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
        methodName = useSetter ? "set${method.capitalize()}" : method
    }

    @Issue("https://github.com/wooga/atlas-github/issues/31")
    def "evaluates body property once"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "$tagName"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()

        buildFile << """
            def callCounter = 0
            
            def publishBodyStrategy = new wooga.gradle.github.publish.PublishBodyStrategy() {
                @Override
                String getBody(org.kohsuke.github.GHRepository ghRepository) {
                    callCounter = callCounter + 1
                    return "evaluate body count: " + callCounter.toString()
                }
            }

            testPublish.body(publishBodyStrategy)
            
            testPublish.doLast {
                println("evaluate body count: " + callCounter.toString())
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("testPublish")

        then:
        hasReleaseByName(versionName)
        def release = getReleaseByName(versionName)

        release.getBody() == "evaluate body count: 1"
        outputContains(result, "evaluate body count: 1")

        where:
        tagName = "v0.1.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')

    }

    @Unroll
    def "can set baseUrl with #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                $methodName("$baseUrl")
                tagName = "$tagName"
                draft = false
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()

        then:
        when:
        runTasksSuccessfully("testPublish")

        then:
        hasRelease(tagName)

        where:
        baseUrl                  | useSetter
        "https://api.github.com" | true
        "https://api.github.com" | false

        methodName = useSetter ? "setBaseUrl" : "baseUrl"
        tagName = "v0.2.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "can set repositoryName with #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                $methodName("$testRepositoryName")
                tagName = "$tagName"
                draft = false
                token = "$testUserToken"
            }            
        """.stripIndent()

        then:
        when:
        runTasksSuccessfully("testPublish")

        then:
        hasRelease(tagName)

        where:
        useSetter << [true, false]
        methodName = useSetter ? "setRepositoryName" : "repositoryName"
        tagName = "v0.2.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "can set tagName with #methodValue and #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                $methodName($preValue "$tagNameValue" $postValue)
                draft = false
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }            
        """.stripIndent()

        when:
        runTasksSuccessfully("testPublish")

        then:
        hasRelease(tagNameValue)

        where:
        tagNameValue          | useSetter | isLazy
        "testReleaseTagOne"   | true      | false
        "testReleaseTagTwo"   | true      | true
        "testReleaseTagThree" | false     | false
        "testReleaseTagFour"  | false     | true

        methodName = useSetter ? "setTagName" : "tagName"
        methodValue = isLazy ? "closure" : "value"
        preValue = isLazy ? "{" : ""
        postValue = isLazy ? "}" : ""
        tagName = "v0.3.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "can set publishMethod with #publishMethod #methodValue and #methodName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "optional release"
        if (publishMethod == PublishMethod.update) {
            createRelease(tagName)
        }

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                $methodName($preValue "$publishMethod" $postValue)
                draft = false
                tagName = "${tagName}"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
            }
        """.stripIndent()

        when:
        runTasksSuccessfully("testPublish")

        then:
        hasRelease(tagName)

        where:
        publishMethod                | useSetter | isLazy
        PublishMethod.create         | true      | false
        PublishMethod.create         | true      | true
        PublishMethod.create         | false     | false
        PublishMethod.create         | false     | true
        PublishMethod.update         | true      | false
        PublishMethod.update         | true      | true
        PublishMethod.update         | false     | false
        PublishMethod.update         | false     | true
        PublishMethod.createOrUpdate | true      | false
        PublishMethod.createOrUpdate | true      | true
        PublishMethod.createOrUpdate | false     | false
        PublishMethod.createOrUpdate | false     | true

        methodName = useSetter ? "setPublishMethod" : "publishMethod"
        methodValue = isLazy ? "closure" : "value"
        preValue = isLazy ? "{" : ""
        postValue = isLazy ? "}" : ""
        tagName = "v0.3.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "can set publishMethod with #publishMethod #methodValue and #methodName Kotlin"() {
        given: "a kotlin buildfile"
        def kotlinBuildFile = createFile("build.gradle.kts", projectDir)
        buildFile.delete()
        kotlinBuildFile << """
            plugins {
                id("net.wooga.github") version "1.3.0"
            }
        """.stripIndent()

        fork = true

        and: "files to publish"
        createTestAssetsToPublish(1)

        and: "optional release"
        if(publishMethod == PublishMethod.update) {
            createRelease(tagName)
        }

        and: "a buildfile with publish task"

        kotlinBuildFile << """
            version = "$versionName"
            
            tasks.register<wooga.gradle.github.publish.tasks.GithubPublish>("testPublish") {
                draft(false)
                tagName("${tagName}")
                repositoryName("$testRepositoryName")
                token("$testUserToken")
            }
        """.stripIndent()

        when:
        def result = runTasks("testPublish")

        then:
        hasRelease(tagName)

        where:
        publishMethod                | useSetter | isLazy
        PublishMethod.create         | true      | false
//        PublishMethod.create         | true      | true
//        PublishMethod.create         | false     | false
//        PublishMethod.create         | false     | true
//        PublishMethod.update         | true      | false
//        PublishMethod.update         | true      | true
//        PublishMethod.update         | false     | false
//        PublishMethod.update         | false     | true
//        PublishMethod.createOrUpdate | true      | false
//        PublishMethod.createOrUpdate | true      | true
//        PublishMethod.createOrUpdate | false     | false
//        PublishMethod.createOrUpdate | false     | true

        methodName = useSetter ? "setPublishMethod" : "publishMethod"
        methodValue = isLazy ? "closure" : "value"
        preValue = isLazy ? "{" : ""
        postValue = isLazy ? "}" : ""
        tagName = "v0.3.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "fails when setting #methodName with invalid repository name #repoName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "0.1.0"

            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "v0.1.0"
                $methodName($repoName)
                token = "$testUserToken"
            }            
        """.stripIndent()

        expect:
        def result = runTasksWithFailure("testPublish")
        outputContains(result, expectedError)

        where:
        repoName                                 | useSetter | expectedError
        "'invalid'"                              | true      | "Repository value $repoName is not a valid github repository name"
        "'invalid'"                              | false     | "Repository value $repoName is not a valid github repository name"
        "'https://github.com/owner/invalid.git'" | true      | "Repository value $repoName is not a valid github repository name"
        "'https://github.com/owner/invalid.git'" | false     | "Repository value $repoName is not a valid github repository name"

        methodName = useSetter ? "setRepositoryName" : "repositoryName"
    }

//    @Unroll
//    def "fails when setting #methodName with invalid token #token"() {
//        given: "files to publish"
//        createTestAssetsToPublish(1)
//
//        and: "a buildfile with publish task"
//        buildFile << """
//            version "0.1.0"
//
//            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
//                from "releaseAssets"
//                tagName = "v0.1.0"
//                $methodName($token)
//                repositoryName = "$testRepositoryName"
//
//            }
//        """.stripIndent()
//
//        expect:
//        def result = runTasksWithFailure("testPublish")
//        outputContains(result, "java.lang.IllegalArgumentException: token")
//
//        where:
//        token | useSetter
//        //"''"  | true
//        //"''"  | false
//        null  | true
//        //null  | false
//
//        methodName = useSetter ? "setToken" : "token"
//    }

//    @Unroll
//    def "fails when setting #methodName with invalid url #url"() {
//        given: "files to publish"
//        createTestAssetsToPublish(1)
//
//        and: "a buildfile with publish task"
//        buildFile << """
//            version "0.1.0"
//
//            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
//                from "releaseAssets"
//                tagName = "v0.1.0"
//                $methodName($url)
//                repositoryName = "$testRepositoryName"
//
//            }
//        """.stripIndent()
//
//        expect:
//        def result = runTasksWithFailure("testPublish")
//        outputContains(result, "java.lang.IllegalArgumentException: baseUrl")
//
//        where:
//        url  | useSetter
//        "''" | true
//        "''" | false
//        null | true
//        null | false
//
//        methodName = useSetter ? "setBaseUrl" : "baseUrl"
//    }

    def "fails when release can't be created with generic exception"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            task testPublish(type:wooga.gradle.github.publish.tasks.GithubPublish) {
                from "releaseAssets"
                tagName = "v0.1    .0"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
                
            }            
        """.stripIndent()

        expect:
        def result = runTasksWithFailure("testPublish")
        outputContains(result, "publish github release failed")
    }
}
