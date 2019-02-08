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

import org.kohsuke.github.GHRelease
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.PagedIterable
import spock.lang.Retry
import spock.lang.Shared

@Retry(mode=Retry.Mode.SETUP_FEATURE_CLEANUP)
abstract class GithubPublishIntegration extends IntegrationSpec {
    String uniquePostfix() {
        String key = "TRAVIS_JOB_NUMBER"
        def env = System.getenv()
        if (env.containsKey(key)) {
            return env.get(key)
        }
        ""
    }

    @Shared
    def testUserName = System.getenv("ATLAS_GITHUB_INTEGRATION_USER")

    @Shared
    def testUserToken = System.getenv("ATLAS_GITHUB_INTEGRATION_PASSWORD")

    @Shared
    def testRepositoryName = "${testUserName}/atlas-github-integration" + uniquePostfix()

    @Shared
    GitHub client

    @Shared
    GHRepository testRepo

    def maybeDelete(String repoName) {
        try {
            def repository = client.getRepository(repoName)
            repository.delete()
        }
        catch (Exception e) {
        }
    }

    def createTestRepo() {
        maybeDelete(testRepositoryName)

        def builder = client.createRepository(testRepositoryName.split('/')[1])
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
        client = GitHub.connectUsingOAuth(testUserToken)
        createTestRepo()
    }

    def setup() {
        buildFile << """
            ${applyPlugin(GithubPlugin)}
        """.stripIndent()
    }

    def cleanup() {
        cleanupReleases()
    }

    void cleanupReleases() {
        try {
            PagedIterable<GHRelease> releases = testRepo.listReleases()
            releases.each {
                it.delete()
            }
        }
        catch(Error e) {

        }

    }

    def cleanupSpec() {
        maybeDelete(testRepositoryName)
    }

    File createTestAssetsToPublish(int numberOfFiles) {
        createTestAssetsToPublish(numberOfFiles, null)
    }

    File createTestAssetsToPublish(int numberOfFiles, String packageName) {
        File assets = new File(projectDir, "releaseAssets")
        assets.mkdirs()
        File packageDirectory = assets

        if (packageName) {
            packageDirectory = new File(assets, packageName)
            packageDirectory.mkdirs()
        }

        numberOfFiles.eachWithIndex { int entry, int i ->
            createFile("file${i}", packageDirectory) << "test content"
        }

        assets
    }

    GHRelease getRelease(String tagName) {
        (GHRelease) testRepo.listReleases().find({ it.tagName == tagName })
    }

    GHRelease getReleaseByName(String name) {
        (GHRelease) testRepo.listReleases().find({ it.name == name })
    }

    Boolean hasRelease(String tagName) {
        getRelease(tagName) != null
    }

    Boolean hasReleaseByName(String name) {
        getReleaseByName(name) != null
    }
}
