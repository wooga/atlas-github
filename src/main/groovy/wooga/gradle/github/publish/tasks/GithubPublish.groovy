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

/**
 * Publish a Github release with or without provided assets.
 * <p>
 * The task implements {@link org.gradle.api.file.CopySourceSpec} and {@link org.gradle.api.tasks.util.PatternFilterable}.
 * Assets to upload can be specified via copy spec syntax.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     githubPublish {
 *         targetCommitish = "master"
 *         tagName = project.version
 *         releaseName = project.version
 *         body = "Release XYZ"
 *         prerelease = false
 *         draft = false
 *
 *         from(file('build/output'))
 *     }
 * }
 */
class GithubPublish extends AbstractGithubTask implements GithubPublishSpec {

    private static final Logger logger = Logging.getLogger(GithubPublish)

    private File assetCollectDirectory
    private File assetUploadDirectory
    private CopySpec assetsCopySpec
    private Boolean processAssets

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

    /**
     * executes the plublish process
     */
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

    private void publishAssets(GHRelease release) {
        assetUploadDirectory.eachFile { File assetFile ->
            def contentType = getAssetContentType(assetFile)
            release.uploadAsset(assetFile, contentType)
        }
    }

    private void prepareAssets() {
        File uploadDir = this.assetUploadDirectory
        assetCollectDirectory.eachFile(FileType.FILES) {
            FileUtils.copyFileToDirectory(it, uploadDir)
        }

        assetCollectDirectory.eachDir {
            def zipFile = new File(uploadDir, it.name + ".zip")
            ZipUtil.pack(it, zipFile, true)
        }
    }

    private GHRelease createGithubRelease(Boolean createDraft) {
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

    private static String getAssetContentType(File assetFile) {
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

    /**
     * Specifies source files or directories for a copy. The given paths are evaluated as per {@link
     * org.gradle.api.Project#files(Object...)}.
     *
     * @param sourcePaths Paths to source files for the copy
     */
    @Override
    GithubPublish from(Object... sourcePaths) {
        assetsCopySpec.from(sourcePaths)
        processAssets = true
        return this
    }

    /**
     * Specifies the source files or directories for a copy and creates a child {@code CopySourceSpec}. The given source
     * path is evaluated as per {@link org.gradle.api.Project#files(Object...)} .
     *
     * @param sourcePath Path to source for the copy
     * @param configureClosure closure for configuring the child CopySourceSpec
     */
    @Override
    GithubPublish from(Object sourcePath, Closure configureClosure) {
        this.from(sourcePath, ConfigureUtil.configureUsing(configureClosure))
        return this
    }

    /**
     * Specifies the source files or directories for a copy and creates a child {@code CopySpec}. The given source
     * path is evaluated as per {@link org.gradle.api.Project#files(Object...)} .
     *
     * @param sourcePath Path to source for the copy
     * @param configureAction action for configuring the child CopySpec
     */
    @Override
    GithubPublish from(Object sourcePath, Action<? super CopySpec> configureAction) {
        assetsCopySpec.from(sourcePath, configureAction)
        processAssets = true
        return this
    }

    /**
     * Returns the set of include patterns.
     *
     * @return The include patterns. Returns an empty set when there are no include patterns.
     */
    @Override
    Set<String> getIncludes() {
        return assetsCopySpec.getIncludes()
    }

    /**
     * Returns the set of include patterns.
     *
     * @return The include patterns. Returns an empty set when there are no include patterns.
     */
    @Override
    Set<String> getExcludes() {
        return assetsCopySpec.getExcludes()
    }

    /**
     * Set the allowable include patterns.  Note that unlike {@link #include(Iterable)} this replaces any previously
     * defined includes.
     *
     * @param includes an Iterable providing new include patterns
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish setIncludes(Iterable<String> includes) {
        assetsCopySpec.setIncludes(includes)
        return this
    }

    /**
     * Set the allowable exclude patterns.  Note that unlike {@link #exclude(Iterable)} this replaces any previously
     * defined excludes.
     *
     * @param excludes an Iterable providing new exclude patterns
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish setExcludes(Iterable<String> excludes) {
        assetsCopySpec.setExcludes(excludes)
        return this
    }

    /**
     * Adds an ANT style include pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * If includes are not provided, then all files in this container will be included. If includes are provided, then a
     * file must match at least one of the include patterns to be processed.
     *
     * @param includes a vararg list of include patterns
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish include(String... includes) {
        assetsCopySpec.include(includes)
        return this
    }

    /**
     * Adds an ANT style include pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * If includes are not provided, then all files in this container will be included. If includes are provided, then a
     * file must match at least one of the include patterns to be processed.
     *
     * @param includes a Iterable providing more include patterns
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish include(Iterable<String> includes) {
        return this.setIncludes(includes)
    }

    /**
     * Adds an include spec. This method may be called multiple times to append new specs.
     *
     * If includes are not provided, then all files in this container will be included. If includes are provided, then a
     * file must match at least one of the include patterns or specs to be included.
     *
     * @param includeSpec the spec to add
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish include(Spec<FileTreeElement> includeSpec) {
        assetsCopySpec.include(includeSpec)
        return this
    }

    /**
     * Adds an include spec. This method may be called multiple times to append new specs. The given closure is passed a
     * {@link org.gradle.api.file.FileTreeElement} as its parameter.
     *
     * If includes are not provided, then all files in this container will be included. If includes are provided, then a
     * file must match at least one of the include patterns or specs to be included.
     *
     * @param includeSpec the spec to add
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish include(Closure includeSpec) {
        return this.include(Specs.<FileTreeElement>convertClosureToSpec(includeSpec))
    }

    /**
     * Adds an ANT style exclude pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not match
     * any exclude pattern to be processed.
     *
     * @param excludes a vararg list of exclude patterns
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish exclude(String... excludes) {
        assetsCopySpec.exclude(excludes)
        return this
    }

    /**
     * Adds an ANT style exclude pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not match
     * any exclude pattern to be processed.
     *
     * @param excludes a Iterable providing new exclude patterns
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish exclude(Iterable<String> excludes) {
        return this.setExcludes(excludes)
    }

    /**
     * Adds an exclude spec. This method may be called multiple times to append new specs.
     *
     * If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not match
     * any exclude pattern to be processed.
     *
     * @param excludeSpec the spec to add
     * @return this
     * @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish exclude(Spec<FileTreeElement> excludeSpec) {
        assetsCopySpec.exclude(excludeSpec)
        return this
    }

    /**
     * Adds an exclude spec. This method may be called multiple times to append new specs.The given closure is passed a
     * {@link org.gradle.api.file.FileTreeElement} as its parameter. The closure should return true or false. Example:
     *
     * <pre autoTested='true'>
     * copySpec {
     *   from 'source'
     *   into 'destination'
     *   //an example of excluding files from certain configuration:
     *   exclude { it.file in configurations.someConf.files }
     * }
     * </pre>
     *
     * If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not match
     * any exclude pattern to be processed.
     *
     * @param excludeSpec the spec to add
     * @return this
     * @see FileTreeElement
     */
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

    /**
     * See: {@link GithubPublishSpec#getTagName()}
     */
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

    /**
     * See: {@link GithubPublishSpec#setTagName(String)}
     */
    @Override
    GithubPublish setTagName(String tagName) {
        this.tagName = tagName
        return this
    }

    /**
     * See: {@link GithubPublishSpec#setTagName(Object)}
     */
    @Override
    GithubPublish setTagName(Object tagName) {
        this.tagName = tagName
        return this
    }

    /**
     * See: {@link GithubPublishSpec#tagName(String)}
     */
    @Override
    GithubPublish tagName(String tagName) {
        return this.setTagName(tagName)
    }

    /**
     * See: {@link GithubPublishSpec#tagName(Object)}
     */
    @Override
    GithubPublish tagName(Object tagName) {
        return this.setTagName(tagName)
    }

    /**
     * See: {@link GithubPublishSpec#getTargetCommitish()}
     */
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

    /**
     * See: {@link GithubPublishSpec#setTargetCommitish(String)}
     */
    @Override
    GithubPublish setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish
        return this
    }

    /**
     * See: {@link GithubPublishSpec#setTargetCommitish(Object)}
     */
    @Override
    GithubPublish setTargetCommitish(Object targetCommitish) {
        this.targetCommitish = targetCommitish
        return this
    }

    /**
     * See: {@link GithubPublishSpec#targetCommitish(String)}
     */
    @Override
    GithubPublish targetCommitish(String targetCommitish) {
        return this.setTargetCommitish(targetCommitish)
    }

    /**
     * See: {@link GithubPublishSpec#targetCommitish(Object)}
     */
    @Override
    GithubPublish targetCommitish(Object targetCommitish) {
        return this.setTargetCommitish(targetCommitish)
    }

    /**
     * See: {@link GithubPublishSpec#getReleaseName()}
     */
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

    /**
     * See: {@link GithubPublishSpec#setReleaseName(String)}
     */
    @Override
    GithubPublish setReleaseName(String name) {
        this.releaseName = name
        return this
    }

    /**
     * See: {@link GithubPublishSpec#setReleaseName(Object)}
     */
    @Override
    GithubPublish setReleaseName(Object name) {
        this.releaseName = name
        return this
    }

    /**
     * See: {@link GithubPublishSpec#releaseName(Object)}
     */
    @Override
    GithubPublish releaseName(Object name) {
        return this.setReleaseName(name)
    }

    /**
     * See: {@link GithubPublishSpec#releaseName(String)}
     */
    @Override
    GithubPublish releaseName(String name) {
        return this.setReleaseName(name)
    }

    /**
     * See: {@link GithubPublishSpec#getBody()}
     */
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

        if (this.body instanceof Callable) {
            return ((Callable) this.body).call().toString()
        }

        return this.body.toString()
    }

    /**
     * See: {@link GithubPublishSpec#setBody(String)}
     */
    @Override
    GithubPublish setBody(String body) {
        this.body = body
        return this
    }

    /**
     * See: {@link GithubPublishSpec#setBody(Object)}
     */
    @Override
    GithubPublish setBody(Object body) {
        this.body = body
        return this
    }

    /**
     * See: {@link GithubPublishSpec#setBody(Closure)}
     */
    @Override
    GithubPublish setBody(Closure closure) {
        if(closure.maximumNumberOfParameters > 1) {
            throw new GradleException("Too many parameters for body clojure")
        }

        this.body = closure
        return this
    }

    /**
     * See: {@link GithubPublishSpec#setBody(PublishBodyStrategy)}
     */
    GithubPublish setBody(PublishBodyStrategy bodyStrategy) {
        this.body = bodyStrategy
        return this
    }

    /**
     * See: {@link GithubPublishSpec#body(String)}
     */
    @Override
    GithubPublish body(String body) {
        return this.setBody(body)
    }

    /**
     * See: {@link GithubPublishSpec#body(Object)}
     */
    @Override
    GithubPublish body(Object body) {
        return this.setBody(body)
    }

    /**
     * See: {@link GithubPublishSpec#body(Closure)}
     */
    @Override
    GithubPublish body(Closure bodyStrategy) {
        return this.setBody(bodyStrategy)
    }

    /**
     * See: {@link GithubPublishSpec#body(PublishBodyStrategy)}
     */
    @Override
    GithubPublish body(PublishBodyStrategy bodyStrategy) {
        return this.setBody(bodyStrategy)
    }

    /**
     * See: {@link GithubPublishSpec#isPrerelease()}
     */
    @Input
    @Override
    boolean isPrerelease() {
        if (this.prerelease instanceof Callable) {
            return ((Callable) this.prerelease).call().asBoolean()
        }

        return this.prerelease.asBoolean()
    }

    /**
     * See: {@link GithubPublishSpec#setPrerelease(boolean)}
     */
    @Override
    GithubPublish setPrerelease(boolean prerelease) {
        this.prerelease = prerelease
        return this
    }

    /**
     * See: {@link GithubPublishSpec#setPrerelease(Object)}
     */
    @Override
    GithubPublish setPrerelease(Object prerelease) {
        this.prerelease = prerelease
        return this
    }

    /**
     * See: {@link GithubPublishSpec#prerelease(boolean)}
     */
    @Override
    GithubPublish prerelease(boolean prerelease) {
        return this.setPrerelease(prerelease)
    }

    /**
     * See: {@link GithubPublishSpec#prerelease(Object)}
     */
    @Override
    GithubPublish prerelease(Object prerelease) {
        return this.setPrerelease(prerelease)
    }

    /**
     * See: {@link GithubPublishSpec#isDraft()}
     */
    @Input
    @Override
    boolean isDraft() {
        if (this.draft instanceof Callable) {
            return ((Callable) this.draft).call().asBoolean()
        }

        return this.draft.asBoolean()
    }

    /**
     * See: {@link GithubPublishSpec#setDraft(boolean)}
     */
    @Override
    GithubPublish setDraft(boolean draft) {
        this.draft = draft
        return this
    }

    /**
     * See: {@link GithubPublishSpec#setDraft(Object)}
     */
    @Override
    GithubPublish setDraft(Object draft) {
        this.draft = draft
        return this
    }

    /**
     * See: {@link GithubPublishSpec#draft(boolean)}
     */
    @Override
    GithubPublish draft(boolean draft) {
        return this.setDraft(draft)
    }

    /**
     * See: {@link GithubPublishSpec#draft(Object)}
     */
    @Override
    GithubPublish draft(Object draft) {
        return this.setDraft(draft)
    }
}
