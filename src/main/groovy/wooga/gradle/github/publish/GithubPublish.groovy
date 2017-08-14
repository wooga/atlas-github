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

package wooga.gradle.github.publish

import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.apache.tika.detect.Detector
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.AutoDetectParser
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.GradleScriptException
import org.gradle.api.file.CopySpec
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.kohsuke.github.*
import org.zeroturnaround.zip.ZipUtil
import wooga.gradle.github.base.GithubRepositoryValidator
import java.util.concurrent.Callable

class GithubPublish extends Copy implements GithubPublishSpec {
    private static final Logger logger = Logging.getLogger(GithubPublish)

    private File assetCollectDirectory
    private File assetUploadDirectory

    protected GitHub getClient() {
        def builder = new GitHubBuilder()

        if (getUserName() && getPassword()) {
            builder = builder.withPassword(getUserName(), getPassword())
        } else if (getUserName() && getToken()) {
            builder = builder.withOAuthToken(getToken(), getUserName())

        } else if (getToken()) {
            builder = builder.withOAuthToken(getToken())

        } else {
            builder = GitHubBuilder.fromCredentials()
        }

        if (getBaseUrl()) {
            builder = builder.withEndpoint(getBaseUrl())
        }

        builder.build()
    }

    GithubPublish() {
        assetCollectDirectory = File.createTempDir("github-publish-collect", name)
        assetUploadDirectory = File.createTempDir("github-publish-prepare", name)
        assetCollectDirectory.deleteOnExit()
        assetUploadDirectory.deleteOnExit()
    }

    @Override
    protected void copy() {

        super.copy()
        if (didWork) {
            setDidWork(false)
            prepareAssets()
            GHRelease release = createGithubRelease()
            try {
                publishAssets(release)
                release.setDraft(isDraft())
            }
            catch (Exception e) {
                release.delete()
                setDidWork(false)
                throw new GradleException("error while uploading assets. Rollback release ${getReleaseName()}")
            }
            setDidWork(true)
        }
    }

    protected void publishAssets(GHRelease release) {
        assetUploadDirectory.eachFile { File assetFile ->
            def contentType = getAssetContentType(assetFile)
            release.uploadAsset(assetFile, contentType)
        }
    }

    protected void prepareAssets() {
        File uploadDir = this.assetUploadDirectory
        assetCollectDirectory.eachFile(FileType.FILES) {
            FileUtils.copyFileToDirectory(it, uploadDir)
        }

        assetCollectDirectory.eachDir {
            def zipFile = new File(uploadDir, it.name + ".zip")
            ZipUtil.pack(it, zipFile, true)
        }
    }

    protected GHRelease createGithubRelease() {
        GitHub client = getClient()
        GHRepository repository = getRepository(client)

        PagedIterable<GHRelease> releases = repository.listReleases()
        if (releases.find { it.tagName == getTagName() }) {
            throw new GradleException("github release with tag ${getTagName()} already exist")
        }

        GHReleaseBuilder builder = repository.createRelease(getTagName())
        builder.draft(true)
        builder.prerelease(isPrerelease())
        builder.commitish(getTargetCommitish())

        if (getBody()) {
            builder.body(getBody())
        }

        if (getReleaseName()) {
            builder.name(getReleaseName())
        }

        return builder.create()
    }

    GHRepository getRepository(GitHub client) {
        GHRepository repository
        try {
            repository = client.getRepository(getRepository())
        }
        catch (Exception e) {
            throw new GradleException("can't find repository ${getRepository()}")
        }
        repository
    }

    String getAssetContentType(File assetFile) {
        InputStream is = new FileInputStream(assetFile)
        BufferedInputStream bis = new BufferedInputStream(is)
        String contentType = "text/plain"
        try {
            AutoDetectParser parser = new AutoDetectParser()
            Detector detector = parser.getDetector()
            Metadata md = new Metadata()
            md.add(Metadata.RESOURCE_NAME_KEY, assetFile.name)
            MediaType mediaType = detector.detect(bis, md)
            contentType = mediaType.toString()
        }
        finally {

        }

        return contentType
    }

    @Override
    File getDestinationDir() {
        return assetCollectDirectory
    }

    @Override
    final void setDestinationDir(File destinationDir) {
        throw new GradleException("method not supported")
    }

    @Override
    AbstractCopyTask into(Object destDir) {
        throw new GradleException("method not supported")
    }

    @Override
    AbstractCopyTask into(Object destPath, Closure configureClosure) {
        throw new GradleException("method not supported")
    }

    @Override
    CopySpec into(Object destPath, Action<? super CopySpec> copySpec) {
        throw new GradleException("method not supported")
    }

    private String repository
    private String baseUrl
    private String userName
    private String password
    private String token

    private Object tagName
    private Object targetCommitish
    private Object releaseName
    private Object body

    private Object prerelease
    private Object draft

    @Override
    String getRepository() {
        return repository
    }

    @Override
    GithubPublish setRepository(String repository) {
        if (repository == null || repository.isEmpty()) {
            throw new IllegalArgumentException("repository")
        }

        if (!GithubRepositoryValidator.validateRepositoryName(repository)) {
            throw new IllegalArgumentException("Repository value '$repository' is not a valid github repository name. Expecting `owner/repo`.")
        }

        this.repository = repository
        return this
    }

    @Override
    GithubPublish repository(String repo) {
        return this.setRepository(repo)
    }

    @Optional
    @Input
    @Override
    String getBaseUrl() {
        return baseUrl
    }

    @Override
    GithubPublish setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl")
        }
        this.baseUrl = baseUrl
        return this
    }

    @Override
    GithubPublishSpec baseUrl(String baseUrl) {
        return this.setBaseUrl(baseUrl)
    }

    @Optional
    @Input
    @Override
    String getToken() {
        return this.token
    }

    @Override
    GithubPublish setToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("token")
        }
        this.token = token
        return this
    }

    @Override
    GithubPublish token(String token) {
        return this.setToken(token)
    }

    @Input
    @Override
    String getTagName() {
        if(this.tagName == null) {
            return null
        }

        if (this.tagName instanceof Callable) {
            return ((Callable) this.tagName).call().toString()
        }

        return this.tagName.toString()
    }

    @Override
    GithubPublish setTagName(String tagName) {
        this.tagName = tagName
        return this
    }

    @Override
    GithubPublishSpec setTagName(Object tagName) {
        this.tagName = tagName
        return this
    }

    @Override
    GithubPublish tagName(String tagName) {
        return this.setTagName(tagName)
    }

    @Override
    GithubPublishSpec tagName(Object tagName) {
        return this.setTagName(tagName)
    }

    @Input
    @Override
    String getTargetCommitish() {
        if(this.targetCommitish == null) {
            return null
        }

        if (this.targetCommitish instanceof Callable) {
            return ((Callable) this.targetCommitish).call().toString()
        }

        return this.targetCommitish.toString()
    }

    @Override
    GithubPublish setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish
        return this
    }

    @Override
    GithubPublishSpec setTargetCommitish(Object targetCommitish) {
        this.targetCommitish = targetCommitish
        return this
    }

    @Override
    GithubPublish targetCommitish(String targetCommitish) {
        return this.setTargetCommitish(targetCommitish)
    }

    @Override
    GithubPublishSpec targetCommitish(Object targetCommitish) {
        return this.setTargetCommitish(targetCommitish)
    }

    @Optional
    @Input
    String getReleaseName() {
        if(this.releaseName == null) {
            return null
        }

        if (this.releaseName instanceof Callable) {
            return ((Callable) this.releaseName).call().toString()
        }

        return this.releaseName.toString()
    }

    @Override
    GithubPublish setReleaseName(String name) {
        this.releaseName = name
        return this
    }

    @Override
    GithubPublishSpec setReleaseName(Object name) {
        this.releaseName = name
        return this
    }

    @Override
    GithubPublishSpec releaseName(Object name) {
        return this.setReleaseName(name)
    }

    @Override
    GithubPublish releaseName(String name) {
        return this.setReleaseName(name)
    }

    @Optional
    @Input
    @Override
    String getBody() {
        if(this.body == null) {
            return null
        }

        if (this.body instanceof Closure) {
            return ((Closure) this.body).call(getRepository(getClient())).toString()
        }

        if (this.body instanceof PublishBodyStrategy) {
            return ((PublishBodyStrategy) this.body).getBody(getRepository(getClient()))
        }

        return this.body.toString()
    }

    @Override
    GithubPublish setBody(String body) {
        this.body = body
        return this
    }

    @Override
    GithubPublishSpec setBody(Object body) {
        this.body = body
        return this
    }

    @Override
    GithubPublishSpec setBody(Closure closure) {
        if(closure.maximumNumberOfParameters > 1) {
            throw new GradleException("Too many parameters for body clojure")
        }

        this.body = closure
        return this
    }

    GithubPublishSpec setBody(PublishBodyStrategy bodyStrategy) {
        this.body = bodyStrategy
        return this
    }


    @Override
    GithubPublish body(String body) {
        return this.setBody(body)
    }

    @Override
    GithubPublishSpec body(Object body) {
        return this.setBody(body)
    }

    @Override
    GithubPublishSpec body(Closure bodyStrategy) {
        return this.setBody(bodyStrategy)
    }

    @Override
    GithubPublishSpec body(PublishBodyStrategy bodyStrategy) {
        return this.setBody(bodyStrategy)
    }

    @Input
    @Override
    boolean isPrerelease() {
        if (this.prerelease instanceof Callable) {
            return ((Callable) this.prerelease).call().asBoolean()
        }

        return this.prerelease.asBoolean()
    }

    @Override
    GithubPublish setPrerelease(boolean prerelease) {
        this.prerelease = prerelease
        return this
    }

    @Override
    GithubPublishSpec setPrerelease(Object prerelease) {
        this.prerelease = prerelease
        return this
    }

    @Override
    GithubPublish prerelease(boolean prerelease) {
        return this.setPrerelease(prerelease)
    }

    @Override
    GithubPublishSpec prerelease(Object prerelease) {
        return this.setPrerelease(prerelease)
    }

    @Input
    @Override
    boolean isDraft() {
        if (this.draft instanceof Callable) {
            return ((Callable) this.draft).call().asBoolean()
        }

        return this.draft.asBoolean()
    }

    @Override
    GithubPublish setDraft(boolean draft) {
        this.draft = draft
        return this
    }

    @Override
    GithubPublishSpec setDraft(Object draft) {
        this.draft = draft
        return this
    }

    @Override
    GithubPublish draft(boolean draft) {
        return this.setDraft(draft)
    }

    @Override
    GithubPublishSpec draft(Object draft) {
        return this.setDraft(draft)
    }

    @Optional
    @Input
    @Override
    String getUserName() {
        return this.userName
    }

    @Override
    GithubPublish setUserName(String userName) {
        this.userName = userName
        return this
    }

    @Override
    GithubPublish userName(String userName) {
        this.setUserName(userName)
        return this
    }

    @Optional
    @Input
    @Override
    String getPassword() {
        return this.password
    }

    @Override
    GithubPublish setPassword(String password) {
        this.password = password
        return this
    }

    @Override
    GithubPublish password(String password) {
        this.setPassword(password)
        return this
    }
}
