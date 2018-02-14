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

package wooga.gradle.github.publish.tasks

import groovy.io.FileType
import org.apache.commons.io.FileUtils
import org.apache.tika.detect.Detector
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.AutoDetectParser
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.specs.Spec
import org.gradle.api.specs.Specs
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WorkResult
import org.gradle.util.ConfigureUtil
import org.kohsuke.github.*
import org.zeroturnaround.zip.ZipUtil
import wooga.gradle.github.base.tasks.internal.AbstractGithubTask
import wooga.gradle.github.publish.GithubPublishSpec
import wooga.gradle.github.publish.PublishBodyStrategy

import java.util.concurrent.Callable

class GithubPublish extends AbstractGithubTask implements GithubPublishSpec {
    private static final Logger logger = Logging.getLogger(GithubPublish)

    private File assetCollectDirectory
    private File assetUploadDirectory

    CopySpec assetsCopySpec

    GithubPublish() {
        super(GithubPublish.class)
        assetsCopySpec = project.copySpec()
        assetCollectDirectory = File.createTempDir("github-publish-collect", name)
        assetUploadDirectory = File.createTempDir("github-publish-prepare", name)
        assetCollectDirectory.deleteOnExit()
        assetUploadDirectory.deleteOnExit()
    }

    File getDestinationDir() {
        assetCollectDirectory
    }

    @TaskAction
    protected void publish() {
        setDidWork(false)
        GHRelease release = createGithubRelease(this.processAssets)

        if (this.processAssets) {
            WorkResult assetCopyResult = project.copy(new Action<CopySpec>()
            {
                @Override
                void execute(CopySpec copySpec) {
                    copySpec.into(getDestinationDir())
                    copySpec.with(assetsCopySpec)
                }
            })

            if (assetCopyResult.didWork) {
                try {
                    prepareAssets()
                    publishAssets(release)
                    release.update().draft(isDraft()).update()
                }
                catch (Exception e) {
                    failRelease(release, "error while uploading assets. Rollback release ${release.name}")
                }
                setDidWork(true)
            } else {
                failRelease(release, "error while preparing assets for upload. Rollback release ${release.name}")
            }
        } else {
            setDidWork(true)
        }
    }

    private void failRelease(GHRelease release, String message) {
        release.delete()
        setDidWork(false)
        throw new GradleException(message)
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

    protected GHRelease createGithubRelease(Boolean createDraft) {
        GitHub client = getClient()
        GHRepository repository = getRepository(client)

        PagedIterable<GHRelease> releases = repository.listReleases()
        if (releases.find { it.tagName == getTagName() }) {
            throw new GradleException("github release with tag ${getTagName()} already exist")
        }

        GHReleaseBuilder builder = repository.createRelease(getTagName())
        builder.draft(createDraft as boolean)
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

    /* CopySpec */

    private Boolean processAssets

    @Override
    GithubPublish from(Object... sourcePaths) {
        assetsCopySpec.from(sourcePaths)
        processAssets = true
        return this
    }

    @Override
    GithubPublish from(Object sourcePath, Closure configureClosure) {
        this.from(sourcePath, ConfigureUtil.configureUsing(configureClosure))
        return this
    }

    @Override
    GithubPublish from(Object sourcePath, Action<? super CopySpec> configureAction) {
        assetsCopySpec.from(sourcePath, configureAction)
        processAssets = true
        return this
    }

    @Override
    Set<String> getIncludes() {
        return assetsCopySpec.getIncludes()
    }

    @Override
    Set<String> getExcludes() {
        return assetsCopySpec.getExcludes()
    }

    @Override
    GithubPublish setIncludes(Iterable<String> includes) {
        assetsCopySpec.setIncludes(includes)
        return this
    }

    @Override
    GithubPublish setExcludes(Iterable<String> excludes) {
        assetsCopySpec.setExcludes(excludes)
        return this
    }

    @Override
    GithubPublish include(String... includes) {
        assetsCopySpec.include(includes)
        return this
    }

    @Override
    GithubPublish include(Iterable<String> includes) {
        return this.setIncludes(includes)
    }

    @Override
    GithubPublish include(Spec<FileTreeElement> includeSpec) {
        assetsCopySpec.include(includeSpec)
        return this
    }

    @Override
    GithubPublish include(Closure includeSpec) {
        return this.include(Specs.<FileTreeElement>convertClosureToSpec(includeSpec))
    }

    @Override
    GithubPublish exclude(String... excludes) {
        assetsCopySpec.exclude(excludes)
        return this
    }

    @Override
    GithubPublish exclude(Iterable<String> excludes) {
        return this.setExcludes(excludes)
    }

    @Override
    GithubPublish exclude(Spec<FileTreeElement> excludeSpec) {
        assetsCopySpec.exclude(excludeSpec)
        return this
    }

    @Override
    GithubPublish exclude(Closure excludeSpec) {
        return this.exclude(Specs.<FileTreeElement>convertClosureToSpec(excludeSpec))
    }

    private Object tagName
    private Object targetCommitish
    private Object releaseName
    private Object body

    private Object prerelease
    private Object draft

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
    GithubPublish setTagName(Object tagName) {
        this.tagName = tagName
        return this
    }

    @Override
    GithubPublish tagName(String tagName) {
        return this.setTagName(tagName)
    }

    @Override
    GithubPublish tagName(Object tagName) {
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
    GithubPublish setTargetCommitish(Object targetCommitish) {
        this.targetCommitish = targetCommitish
        return this
    }

    @Override
    GithubPublish targetCommitish(String targetCommitish) {
        return this.setTargetCommitish(targetCommitish)
    }

    @Override
    GithubPublish targetCommitish(Object targetCommitish) {
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
    GithubPublish setReleaseName(Object name) {
        this.releaseName = name
        return this
    }

    @Override
    GithubPublish releaseName(Object name) {
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
    GithubPublish setBody(Object body) {
        this.body = body
        return this
    }

    @Override
    GithubPublish setBody(Closure closure) {
        if(closure.maximumNumberOfParameters > 1) {
            throw new GradleException("Too many parameters for body clojure")
        }

        this.body = closure
        return this
    }

    GithubPublish setBody(PublishBodyStrategy bodyStrategy) {
        this.body = bodyStrategy
        return this
    }


    @Override
    GithubPublish body(String body) {
        return this.setBody(body)
    }

    @Override
    GithubPublish body(Object body) {
        return this.setBody(body)
    }

    @Override
    GithubPublish body(Closure bodyStrategy) {
        return this.setBody(bodyStrategy)
    }

    @Override
    GithubPublish body(PublishBodyStrategy bodyStrategy) {
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
    GithubPublish setPrerelease(Object prerelease) {
        this.prerelease = prerelease
        return this
    }

    @Override
    GithubPublish prerelease(boolean prerelease) {
        return this.setPrerelease(prerelease)
    }

    @Override
    GithubPublish prerelease(Object prerelease) {
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
    GithubPublish setDraft(Object draft) {
        this.draft = draft
        return this
    }

    @Override
    GithubPublish draft(boolean draft) {
        return this.setDraft(draft)
    }

    @Override
    GithubPublish draft(Object draft) {
        return this.setDraft(draft)
    }
}
