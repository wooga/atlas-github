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

import com.wooga.spock.extensions.github.GithubRepository
import com.wooga.spock.extensions.github.Repository
import com.wooga.spock.extensions.github.api.RateLimitHandlerWait
import com.wooga.spock.extensions.github.api.TravisBuildNumberPostFix
import org.kohsuke.github.GHContentUpdateResponse
import org.kohsuke.github.GHRelease
import org.kohsuke.github.GitHub
import spock.lang.Retry
import spock.lang.Shared

@Retry(mode=Retry.Mode.SETUP_FEATURE_CLEANUP)
abstract class GithubPublishIntegration extends IntegrationSpec {

    @Shared
    @GithubRepository(
            usernameEnv = "ATLAS_GITHUB_INTEGRATION_USER",
            tokenEnv = "ATLAS_GITHUB_INTEGRATION_PASSWORD",
            repositoryPostFixProvider = [TravisBuildNumberPostFix.class],
            rateLimitHandler = RateLimitHandlerWait.class,
            resetAfterTestCase = true
    )
    Repository testRepo

    def setup() {
        buildFile << """
            ${applyPlugin(GithubPlugin)}
        """.stripIndent()
    }

    def maybeDelete(String repoName) {
        try {
            def repository = client.getRepository(repoName)
            repository.delete()
        }
        catch (Exception e) {
        }
    }

    GHContentUpdateResponse createContent(String content, String commitMessage, String path) throws IOException {
        return testRepo.createContent().content(content).message(commitMessage).path(path).commit();
    }

    GitHub getClient() {
        testRepo.client
    }

    def createRelease(String tagName) {
        testRepo.createRelease(tagName,tagName)
    }

    void cleanupReleases() {
        testRepo.cleanupReleases()
    }

    String getTestUserName() {
        testRepo.userName
    }

    String getTestUserToken() {
        testRepo.token
    }

    String getTestRepositoryName() {
        testRepo.fullName
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
