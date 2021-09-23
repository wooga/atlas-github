/*
 * Copyright 2018-2021 Wooga GmbH
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

package wooga.gradle.github.tasks

import spock.lang.Ignore
import spock.lang.Retry
import spock.lang.Unroll
import wooga.gradle.github.publish.PublishMethod
import wooga.gradle.github.publish.tasks.GithubPublish
import wooga.gradle.github.tasks.AbstractGithubTaskIntegrationSpec

@Retry(mode = Retry.Mode.SETUP_FEATURE_CLEANUP)
class GithubPublishTaskIntegrationSpec extends AbstractGithubTaskIntegrationSpec {

    String testTaskName = "publishTestTask"
    Class testTaskType = GithubPublish

    def "task skips if repository is not set"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            ${testTaskName} {
                from "releaseAssets"
                tagName = "v0.1.0"
            }
        """.stripIndent()

        when:
        def result = runTasksSuccessfully(testTaskName)

        then:
        result.wasSkipped(testTaskName)
    }

    @Unroll("can set property #property with #method and type #type")
    def "can set property"() {

        given: "a task to read back the value"
        buildFile << """
            task("readValue") {
                doLast {
                    println("property: " + ${testTaskName}.${property}.get())
                }
            }
        """.stripIndent()

        and: "a set property"
        buildFile << """
            ${testTaskName}.${method}($value)
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("readValue")

        then:
        outputContains(result, "property: " + expectedValue.toString())

        where:
        property          | method                | rawValue                     | type
        "draft"           | "draft.set"           | true                         | "Boolean"
        "draft"           | "draft.set"           | false                        | "Provider<Boolean>"
        "draft"           | "setDraft"            | true                         | "Provider<Boolean>"

        "prerelease"      | "prerelease.set"      | false                        | "Boolean"
        "prerelease"      | "prerelease.set"      | true                         | "Provider<Boolean>"
        "prerelease"      | "setPrerelease"       | false                        | "Provider<Boolean>"

        "targetCommitish" | "targetCommitish.set" | "foo"                        | "String"
        "targetCommitish" | "targetCommitish.set" | "bar"                        | "Provider<String>"
        "targetCommitish" | "setTargetCommitish"  | "foobar"                     | "Provider<String>"

        "releaseName"     | "releaseName.set"     | "foo"                        | "String"
        "releaseName"     | "releaseName.set"     | "bar"                        | "Provider<String>"
        "releaseName"     | "setReleaseName"      | "foobar"                     | "Provider<String>"

        "tagName"         | "tagName.set"         | "foo"                        | "String"
        "tagName"         | "tagName.set"         | "bar"                        | "Provider<String>"
        "tagName"         | "setTagName"          | "foobar"                     | "Provider<String>"

        "body"            | "body.set"            | "foo"                        | "String"
        "body"            | "body.set"            | "bar"                        | "Provider<String>"
        "body"            | "body.set"            | "foo"                        | "String"

        "publishMethod"   | "publishMethod.set"   | PublishMethod.create         | "Provider<PublishMethod>"
        "publishMethod"   | "publishMethod.set"   | PublishMethod.update         | "Provider<PublishMethod>"
        "publishMethod"   | "publishMethod.set"   | PublishMethod.createOrUpdate | "Provider<PublishMethod>"
        "publishMethod"   | "setPublishMethod"    | PublishMethod.create         | "Provider<PublishMethod>"
        "publishMethod"   | "setPublishMethod"    | PublishMethod.update         | "Provider<PublishMethod>"
        "publishMethod"   | "setPublishMethod"    | PublishMethod.createOrUpdate | "Provider<PublishMethod>"
        "publishMethod"   | "setPublishMethod"    | "create"                     | "String"
        "publishMethod"   | "setPublishMethod"    | "update"                     | "String"
        "publishMethod"   | "setPublishMethod"    | "createOrUpdate"             | "String"

        value = wrapValueBasedOnType(rawValue, type.toString()) { type ->
            switch (type) {
                case PublishMethod.class.simpleName:
                    return "${PublishMethod.class.name}.${rawValue.toString()}"
                default:
                    return rawValue
            }

        }
        expectedValue = rawValue
    }

    @Unroll
    def "can set repositoryName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            ${testTaskName} {
                from "releaseAssets"
                repositoryName = "$testRepositoryName"
                tagName = "$tagName"
                draft = false
                token = "$testUserToken"
            }            
        """.stripIndent()

        then:
        when:
        runTasksSuccessfully(testTaskName)

        then:
        hasRelease(tagName)

        where:
        tagName = "v0.2.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll("task of type GithubPublish with publishMethod #publishMethod will #message")
    def "publishMethod will create or update release"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "optional release"
        if (releaseAlreadyCreated) {
            createRelease(tagName.toString(), releaseBodyBeforeUpdate.toString())
        }

        and: "a buildfile with publish task"
        buildFile << """
            version "$versionName"

            ${testTaskName} {
                from "releaseAssets"
                publishMethod = "$publishMethod"
                draft = false
                tagName = "${tagName}"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
                body = "${releaseBody}"
            }
        """.stripIndent()

        when:
        def result = runTasks(testTaskName)

        then:
        result.success != expectedFailure

        if(result.success) {
            throttle()
            assert hasRelease(tagName)
            def release = getRelease(tagName)
            assert release.body == releaseBody
        }

        where:
        publishMethod                | releaseAlreadyCreated | releaseBodyBeforeUpdate | releaseBody       | expectedFailure | message
        PublishMethod.create         | false                 | _                       | "release created" | false           | "create a new release"
        PublishMethod.create         | true                  | "release created"       | _                 | true            | "fail when release already exists"

        PublishMethod.update         | false                 | _                       | _                 | true            | "fail when release to update doesn't exist"
        PublishMethod.update         | true                  | "release created"       | "release updated" | false           | "update release"

        PublishMethod.createOrUpdate | true                  | "release created"       | "release updated" | false           | "update release when release exists"
        PublishMethod.createOrUpdate | false                 | _                       | "release updated" | false           | "create release when release doesn't exist"

        tagName = "v0.3.${Math.abs(new Random().nextInt() % 1000) + 1}-GithubPublishPropertySpec"
        versionName = tagName.replaceFirst('v', '')
    }

    @Unroll
    def "fails when setting task with invalid repository name #repoName"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "0.1.0"

            ${testTaskName} {
                from "releaseAssets"
                tagName = "v0.1.0"
                repositoryName = $repoName
                token = "$testUserToken"
            }
        
        """.stripIndent()

        expect:
        def result = runTasksWithFailure(testTaskName)
        outputContains(result, expectedError)

        where:
        repoName                                 | expectedError
        "'invalid'"                              | "Repository value $repoName is not a valid github repository name"
        "'https://github.com/owner/invalid.git'" | "Repository value $repoName is not a valid github repository name"

    }

    @Ignore
    @Unroll
    def "fails when setting token with invalid value #token"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "0.1.0"

             ${testTaskName} {
                from "releaseAssets"
                tagName = "v0.1.0"
                token = $token
                repositoryName = "$testRepositoryName"

            }
        """.stripIndent()

        expect:
        def result = runTasksWithFailure(testTaskName)
        outputContains(result, "java.lang.IllegalArgumentException: token")

        where:
        token << ["''", "null"]
    }

    @Ignore
    @Unroll
    def "fails when setting #methodName with invalid url #url"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            version "0.1.0"

             ${testTaskName} {
                from "releaseAssets"
                tagName = "v0.1.0"
                baseUrl = $url
                repositoryName = "$testRepositoryName"

            }
        """.stripIndent()

        expect:
        def result = runTasksWithFailure(testTaskName)
        outputContains(result, "java.lang.IllegalArgumentException: baseUrl")

        where:
        url << ["''", null]
    }

    def "fails when release can't be created with generic exception"() {
        given: "files to publish"
        createTestAssetsToPublish(1)

        and: "a buildfile with publish task"
        buildFile << """
            ${testTaskName} {
                from "releaseAssets"
                tagName = "v0.1    .0"
                repositoryName = "$testRepositoryName"
                token = "$testUserToken"
                
            }            
        """.stripIndent()

        expect:
        def result = runTasksWithFailure(testTaskName)
        outputContains(result, "publish github release failed")
    }
}
