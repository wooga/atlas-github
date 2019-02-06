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

package wooga.gradle.github.publish.tasks

import groovy.io.FileType
import groovy.json.JsonSlurper
import org.apache.commons.io.FileUtils
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
import wooga.gradle.github.publish.PublishMethod
import wooga.gradle.github.publish.internal.GithubReleasePropertySetter
import wooga.gradle.github.publish.internal.GithubReleaseCreateException
import wooga.gradle.github.publish.internal.GithubReleasePublishException
import wooga.gradle.github.publish.internal.GithubReleaseUpdateException
import wooga.gradle.github.publish.internal.GithubReleaseUploadAssetException
import wooga.gradle.github.publish.internal.GithubReleaseUploadAssetsException
import wooga.gradle.github.publish.internal.ReleaseAssetUpload

import java.util.concurrent.Callable

/**
 * Publish a Github release with or without provided assets.
 * <p>
 * The task implements {@link org.gradle.api.file.CopySourceSpec} and {@link org.gradle.api.tasks.util.PatternFilterable}.
 * Assets to restore can be specified via copy spec syntax.
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
 *         publishMethod = "create"
 *         from(file('build/output'))
 *     }
 *}
 */
class GithubPublish extends AbstractGithubTask implements GithubPublishSpec {

    private static final Logger logger = Logging.getLogger(GithubPublish)

    private File assetCollectDirectory
    private File assetUploadDirectory
    protected CopySpec assetsCopySpec
    private Boolean processAssets
    private Boolean isNewlyCreatedRelease = false

    GithubPublish() {
        super(GithubPublish.class)
        assetsCopySpec = project.copySpec()

        assetCollectDirectory = project.file("${temporaryDir}/collect")
        assetUploadDirectory = project.file("${temporaryDir}/prepare")
    }

    File getDestinationDir() {
        assetCollectDirectory
    }

    /**
     * executes the publish process
     */
    @TaskAction
    protected void publish() {
        try {
            GHRelease release = createOrUpdateGithubRelease(this.processAssets || isDraft())
            if (this.processAssets) {
                processReleaseAssets(release)
            }
        } catch (GithubReleasePublishException releaseError) {
            failRelease(releaseError.getRelease(), releaseError.message, isNewlyCreatedRelease, releaseError)
        }
        setDidWork(true)
    }

    protected void processReleaseAssets(GHRelease release) throws GithubReleaseUploadAssetsException, GithubReleaseUpdateException{
        WorkResult assetCopyResult = project.sync(new Action<CopySpec>()
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
            }
            catch (Exception error) {
                throw new GithubReleaseUploadAssetsException(release, "error while uploading assets.", error)
            }

            try {
                if (release.draft != isDraft()) {
                    release.update().draft(isDraft()).tag(getTagName()).update()
                }
            } catch (Exception error) {
                throw new GithubReleaseUpdateException(release, "error while publishing draft", error)
            }
        } else {
            throw new GithubReleaseUploadAssetsException(release, "error while preparing assets for restore")
        }
    }

    protected void failRelease(GHRelease release, String message, boolean deleteRelease, Throwable cause = null) {
        setDidWork(false)
        logger.error("publish github release failed")
        logger.error("attempt rollback")
        if (deleteRelease && release) {
            logger.info("delete created release")
            try {
                release.delete()
            } catch (Exception error) {
                logger.error("failed to rollback release")
                logger.error(error.message)
            }
        }

        if(cause && cause.cause && GithubReleaseUploadAssetException.isInstance(cause.cause) && !deleteRelease) {
            GithubReleaseUploadAssetException rootCause = cause.cause as GithubReleaseUploadAssetException
            rootCause.uploadedAssets.each {
                try {
                    logger.info("delete published asset ${it.name}")
                    it.delete()
                } catch (Exception error) {
                    logger.error("failed to rollback asset ${it.name}")
                    logger.error(error.message)
                }
            }

            rootCause.updatedAssets.each {
                try {
                    logger.info("restore updated asset ${it.name}")
                    it.restore(release)
                } catch (Exception error) {
                    logger.error("failed to restore asset ${it.name}")
                    logger.error(error.message)
                }
            }
        }

        setDidWork(false)
        throw new GradleException(message, cause)
    }

    static class UpdatedAsset {
        private String name
        private String contentType
        private File file


        UpdatedAsset(String name, String contentType, File file) {
            this.name = name
            this.contentType = contentType
            this.file = file
        }

        static UpdatedAsset fromAsset(GHAsset asset) {
            File tempFile = File.createTempFile(asset.name, "update_asset")
            def assetURL = new URL(asset.browserDownloadUrl)
            assetURL.withInputStream { i ->
                tempFile.withOutputStream {
                    it << i
                }
            }

            new UpdatedAsset(asset.name, asset.contentType, tempFile)
        }

        GHAsset restore(GHRelease release) {
            ReleaseAssetUpload.uploadAssetRetry(release, name, new FileInputStream(file), contentType)
        }
    }

    protected void publishAssets(GHRelease release) throws GithubReleaseUploadAssetException {
        List<GHAsset> publishedAssets = []
        List<UpdatedAsset> updatedAssets = []

        assetUploadDirectory.listFiles().findAll {it.isFile()}.sort().each { File assetFile ->
            try {
                publishedAssets << ReleaseAssetUpload.uploadAsset(release, assetFile)
            } catch (HttpException httpError) {
                if( isDuplicateAssetError(httpError)) {
                    logger.info("asset ${assetFile.name} already published")
                    logger.info("attempt override")
                    def duplicateAsset = release.assets.find {it.name == assetFile.name}
                    if(duplicateAsset) {
                        try {
                            UpdatedAsset updatedAsset = UpdatedAsset.fromAsset(duplicateAsset)
                            duplicateAsset.delete()
                            updatedAssets << updatedAsset
                            publishedAssets << ReleaseAssetUpload.uploadAsset(release, assetFile)
                        } catch(Exception e) {
                            logger.error("failure during update of duplicate asset ${assetFile.name}")
                            logger.error(e.message)
                            logger.info("fail with original error")
                            throw new GithubReleaseUploadAssetException(publishedAssets, updatedAssets, e)
                        }
                    } else {
                        logger.error("unable to find duplicate asset ${assetFile.name} in release assets")
                        logger.error("this could mean the asset contains special characters!")
                        throw new GithubReleaseUploadAssetException(publishedAssets, updatedAssets, httpError)
                    }
                } else {
                    throw new GithubReleaseUploadAssetException(publishedAssets, updatedAssets, httpError)
                }
            }
        }
    }

    protected static Boolean isDuplicateAssetError(HttpException httpError) {
        Boolean status = false
        if (httpError.responseCode == 422) {
            def json = new JsonSlurper()
            Map details = json.parse(httpError.getMessage().chars) as Map
            List<Map> errors = details["errors"] as List<Map>
            if (errors && errors.first() && errors.first()["code"] == "already_exists") {
                status = true
            }
        }

        return status
    }

    protected void prepareAssets() {
        File uploadDir = this.assetUploadDirectory
        uploadDir.deleteDir()
        uploadDir.mkdirs()

        assetCollectDirectory.eachFile(FileType.FILES) {
            FileUtils.copyFileToDirectory(it, uploadDir)
        }

        assetCollectDirectory.eachDir {
            def zipFile = new File(uploadDir, it.name + ".zip")
            ZipUtil.pack(it, zipFile, true)
        }
    }

    protected GHRelease createOrUpdateGithubRelease(Boolean createDraft) throws GithubReleaseUpdateException, GithubReleaseCreateException {
        GitHub client = getClient()
        GHRepository repository = getRepository(client)

        GHRelease release = repository.listReleases().find({ it.tagName == getTagName() }) as GHRelease
        GithubReleasePropertySetter releasePropertySet
        GHRelease result
        if (release) {
            if (this.getPublishMethod() == PublishMethod.create) {
                throw new GithubReleaseCreateException("github release with tag ${getTagName()} already exist")
            }

            releasePropertySet = new GithubReleasePropertySetter(release.update())
            releasePropertySet.draft(isDraft())

            try {
                result = setReleasePropertiesAndCommit(releasePropertySet)
            } catch (Exception error) {
                throw new GithubReleaseUpdateException("failed to update release ${release.tagName}", error)
            }
        } else {
            if (this.getPublishMethod() == PublishMethod.update) {
                throw new GithubReleaseUpdateException("github release with tag ${getTagName()} for update not found")
            }

            isNewlyCreatedRelease = true
            releasePropertySet = new GithubReleasePropertySetter(repository.createRelease(getTagName()))
            releasePropertySet.draft(createDraft as boolean)

            try {
                result = setReleasePropertiesAndCommit(releasePropertySet)
            } catch (Exception error) {
                throw new GithubReleaseCreateException("failed to create release ${release.tagName}", error)
            }
        }
        result
    }

    protected GHRelease setReleasePropertiesAndCommit(GithubReleasePropertySetter releasePropertySet) {
        releasePropertySet.prerelease(isPrerelease())

        if (getTargetCommitish()) {
            releasePropertySet.commitish(getTargetCommitish())
        }

        if (getBody()) {
            releasePropertySet.body(getBody())
        }

        if (getReleaseName()) {
            releasePropertySet.name(getReleaseName())
        }

        releasePropertySet.commit()
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
        this
    }

    /**
     * Specifies the source files or directories for a copy and creates a child {@code CopySourceSpec}. The given source
     * path is evaluated as per {@link org.gradle.api.Project#files(Object ...)} .
     *
     * @param sourcePath Path to source for the copy
     * @param configureClosure closure for configuring the child CopySourceSpec
     */
    @Override
    GithubPublish from(Object sourcePath, Closure configureClosure) {
        this.from(sourcePath, ConfigureUtil.configureUsing(configureClosure))
        this
    }

    /**
     * Specifies the source files or directories for a copy and creates a child {@code CopySpec}. The given source
     * path is evaluated as per {@link org.gradle.api.Project#files(Object ...)} .
     *
     * @param sourcePath Path to source for the copy
     * @param configureAction action for configuring the child CopySpec
     */
    @Override
    GithubPublish from(Object sourcePath, Action<? super CopySpec> configureAction) {
        assetsCopySpec.from(sourcePath, configureAction)
        processAssets = true
        this
    }

    /**
     * Returns the set of include patterns.
     *
     * @return The include patterns. Returns an empty set when there are no include patterns.
     */
    @Override
    Set<String> getIncludes() {
        assetsCopySpec.getIncludes()
    }

    /**
     * Returns the set of include patterns.
     *
     * @return The include patterns. Returns an empty set when there are no include patterns.
     */
    @Override
    Set<String> getExcludes() {
        assetsCopySpec.getExcludes()
    }

    /**
     * Set the allowable include patterns.  Note that unlike {@link #include(Iterable)} this replaces any previously
     * defined includes.
     *
     * @param includes an Iterable providing new include patterns
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish setIncludes(Iterable<String> includes) {
        assetsCopySpec.setIncludes(includes)
        this
    }

    /**
     * Set the allowable exclude patterns.  Note that unlike {@link #exclude(Iterable)} this replaces any previously
     * defined excludes.
     *
     * @param excludes an Iterable providing new exclude patterns
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish setExcludes(Iterable<String> excludes) {
        assetsCopySpec.setExcludes(excludes)
        this
    }

    /**
     * Adds an ANT style include pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * If includes are not provided, then all files in this container will be included. If includes are provided, then a
     * file must match at least one of the include patterns to be processed.
     *
     * @param includes a vararg list of include patterns
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish include(String... includes) {
        assetsCopySpec.include(includes)
        this
    }

    /**
     * Adds an ANT style include pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * If includes are not provided, then all files in this container will be included. If includes are provided, then a
     * file must match at least one of the include patterns to be processed.
     *
     * @param includes a Iterable providing more include patterns
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish include(Iterable<String> includes) {
        this.setIncludes(includes)
    }

    /**
     * Adds an include spec. This method may be called multiple times to append new specs.
     *
     * If includes are not provided, then all files in this container will be included. If includes are provided, then a
     * file must match at least one of the include patterns or specs to be included.
     *
     * @param includeSpec the spec to add
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish include(Spec<FileTreeElement> includeSpec) {
        assetsCopySpec.include(includeSpec)
        this
    }

    /**
     * Adds an include spec. This method may be called multiple times to append new specs. The given closure is passed a
     * {@link org.gradle.api.file.FileTreeElement} as its parameter.
     *
     * If includes are not provided, then all files in this container will be included. If includes are provided, then a
     * file must match at least one of the include patterns or specs to be included.
     *
     * @param includeSpec the spec to add
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish include(Closure includeSpec) {
        this.include(Specs.<FileTreeElement> convertClosureToSpec(includeSpec))
    }

    /**
     * Adds an ANT style exclude pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not match
     * any exclude pattern to be processed.
     *
     * @param excludes a vararg list of exclude patterns
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish exclude(String... excludes) {
        assetsCopySpec.exclude(excludes)
        this
    }

    /**
     * Adds an ANT style exclude pattern. This method may be called multiple times to append new patterns and multiple
     * patterns may be specified in a single call.
     *
     * If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not match
     * any exclude pattern to be processed.
     *
     * @param excludes a Iterable providing new exclude patterns
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish exclude(Iterable<String> excludes) {
        this.setExcludes(excludes)
    }

    /**
     * Adds an exclude spec. This method may be called multiple times to append new specs.
     *
     * If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not match
     * any exclude pattern to be processed.
     *
     * @param excludeSpec the spec to add
     * @return this* @see org.gradle.api.tasks.util.PatternFilterable Pattern Format
     */
    @Override
    GithubPublish exclude(Spec<FileTreeElement> excludeSpec) {
        assetsCopySpec.exclude(excludeSpec)
        this
    }

    /**
     * Adds an exclude spec. This method may be called multiple times to append new specs.The given closure is passed a
     * {@link org.gradle.api.file.FileTreeElement} as its parameter. The closure should return true or false. Example:
     *
     * <pre autoTested='true'>
     * copySpec {*   from 'source'
     *   into 'destination'
     *   //an example of excluding files from certain configuration:
     *   exclude { it.file in configurations.someConf.files }*}* </pre>
     *
     * If excludes are not provided, then no files will be excluded. If excludes are provided, then files must not match
     * any exclude pattern to be processed.
     *
     * @param excludeSpec the spec to add
     * @return this* @see FileTreeElement
     */
    @Override
    GithubPublish exclude(Closure excludeSpec) {
        this.exclude(Specs.<FileTreeElement> convertClosureToSpec(excludeSpec))
    }

    private Object tagName
    private Object targetCommitish
    private Object releaseName
    private Object body

    private Object prerelease
    private Object draft
    private Object publishMethod = PublishMethod.create

    /**
     * See: {@link GithubPublishSpec#getTagName()}
     */
    @Input
    @Override
    String getTagName() {
        if (this.tagName == null) {
            return null
        }

        if (this.tagName instanceof Callable) {
            return ((Callable) this.tagName).call().toString()
        }

        this.tagName.toString()
    }

    /**
     * See: {@link GithubPublishSpec#setTagName(String)}
     */
    @Override
    GithubPublish setTagName(String tagName) {
        this.tagName = tagName
        this
    }

    /**
     * See: {@link GithubPublishSpec#setTagName(Object)}
     */
    @Override
    GithubPublish setTagName(Object tagName) {
        this.tagName = tagName
        this
    }

    /**
     * See: {@link GithubPublishSpec#tagName(String)}
     */
    @Override
    GithubPublish tagName(String tagName) {
        this.setTagName(tagName)
    }

    /**
     * See: {@link GithubPublishSpec#tagName(Object)}
     */
    @Override
    GithubPublish tagName(Object tagName) {
        this.setTagName(tagName)
    }

    /**
     * See: {@link GithubPublishSpec#getTargetCommitish()}
     */
    @Input
    @Optional
    @Override
    String getTargetCommitish() {
        if (this.targetCommitish == null) {
            return null
        }

        if (this.targetCommitish instanceof Callable) {
            return ((Callable) this.targetCommitish).call().toString()
        }

        this.targetCommitish.toString()
    }

    /**
     * See: {@link GithubPublishSpec#setTargetCommitish(String)}
     */
    @Override
    GithubPublish setTargetCommitish(String targetCommitish) {
        this.targetCommitish = targetCommitish
        this
    }

    /**
     * See: {@link GithubPublishSpec#setTargetCommitish(Object)}
     */
    @Override
    GithubPublish setTargetCommitish(Object targetCommitish) {
        this.targetCommitish = targetCommitish
        this
    }

    /**
     * See: {@link GithubPublishSpec#targetCommitish(String)}
     */
    @Override
    GithubPublish targetCommitish(String targetCommitish) {
        this.setTargetCommitish(targetCommitish)
    }

    /**
     * See: {@link GithubPublishSpec#targetCommitish(Object)}
     */
    @Override
    GithubPublish targetCommitish(Object targetCommitish) {
        this.setTargetCommitish(targetCommitish)
    }

    /**
     * See: {@link GithubPublishSpec#getReleaseName()}
     */
    @Optional
    @Input
    String getReleaseName() {
        if (this.releaseName == null) {
            return null
        }

        if (this.releaseName instanceof Callable) {
            return ((Callable) this.releaseName).call().toString()
        }

        this.releaseName.toString()
    }

    /**
     * See: {@link GithubPublishSpec#setReleaseName(String)}
     */
    @Override
    GithubPublish setReleaseName(String name) {
        this.releaseName = name
        this
    }

    /**
     * See: {@link GithubPublishSpec#setReleaseName(Object)}
     */
    @Override
    GithubPublish setReleaseName(Object name) {
        this.releaseName = name
        this
    }

    /**
     * See: {@link GithubPublishSpec#releaseName(Object)}
     */
    @Override
    GithubPublish releaseName(Object name) {
        this.setReleaseName(name)
    }

    /**
     * See: {@link GithubPublishSpec#releaseName(String)}
     */
    @Override
    GithubPublish releaseName(String name) {
        this.setReleaseName(name)
    }

    /**
     * See: {@link GithubPublishSpec#getBody()}
     */
    @Optional
    @Input
    @Override
    String getBody() {
        if (this.body == null) {
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

        this.body.toString()
    }

    /**
     * See: {@link GithubPublishSpec#setBody(String)}
     */
    @Override
    GithubPublish setBody(String body) {
        this.body = body
        this
    }

    /**
     * See: {@link GithubPublishSpec#setBody(Object)}
     */
    @Override
    GithubPublish setBody(Object body) {
        this.body = body
        this
    }

    /**
     * See: {@link GithubPublishSpec#setBody(Closure)}
     */
    @Override
    GithubPublish setBody(Closure closure) {
        if (closure.maximumNumberOfParameters > 1) {
            throw new GradleException("Too many parameters for body clojure")
        }

        this.body = closure
        this
    }

    /**
     * See: {@link GithubPublishSpec#setBody(PublishBodyStrategy)}
     */
    GithubPublish setBody(PublishBodyStrategy bodyStrategy) {
        this.body = bodyStrategy
        this
    }

    /**
     * See: {@link GithubPublishSpec#body(String)}
     */
    @Override
    GithubPublish body(String body) {
        this.setBody(body)
    }

    /**
     * See: {@link GithubPublishSpec#body(Object)}
     */
    @Override
    GithubPublish body(Object body) {
        this.setBody(body)
    }

    /**
     * See: {@link GithubPublishSpec#body(Closure)}
     */
    @Override
    GithubPublish body(Closure bodyStrategy) {
        this.setBody(bodyStrategy)
    }

    /**
     * See: {@link GithubPublishSpec#body(PublishBodyStrategy)}
     */
    @Override
    GithubPublish body(PublishBodyStrategy bodyStrategy) {
        this.setBody(bodyStrategy)
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

        this.prerelease.asBoolean()
    }

    /**
     * See: {@link GithubPublishSpec#setPrerelease(boolean)}
     */
    @Override
    GithubPublish setPrerelease(boolean prerelease) {
        this.prerelease = prerelease
        this
    }

    /**
     * See: {@link GithubPublishSpec#setPrerelease(Object)}
     */
    @Override
    GithubPublish setPrerelease(Object prerelease) {
        this.prerelease = prerelease
        this
    }

    /**
     * See: {@link GithubPublishSpec#prerelease(boolean)}
     */
    @Override
    GithubPublish prerelease(boolean prerelease) {
        this.setPrerelease(prerelease)
    }

    /**
     * See: {@link GithubPublishSpec#prerelease(Object)}
     */
    @Override
    GithubPublish prerelease(Object prerelease) {
        this.setPrerelease(prerelease)
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

        this.draft.asBoolean()
    }

    /**
     * See: {@link GithubPublishSpec#setDraft(boolean)}
     */
    @Override
    GithubPublish setDraft(boolean draft) {
        this.draft = draft
        this
    }

    /**
     * See: {@link GithubPublishSpec#setDraft(Object)}
     */
    @Override
    GithubPublish setDraft(Object draft) {
        this.draft = draft
        this
    }

    /**
     * See: {@link GithubPublishSpec#draft(boolean)}
     */
    @Override
    GithubPublish draft(boolean draft) {
        this.setDraft(draft)
    }

    /**
     * See: {@link GithubPublishSpec#draft(Object)}
     */
    @Override
    GithubPublish draft(Object draft) {
        this.setDraft(draft)
    }

    /**
     * See: {@link GithubPublishSpec#getPublishMethod()}
     */
    @Override
    PublishMethod getPublishMethod() {
        if (this.publishMethod instanceof Callable) {
            return ((Callable) this.publishMethod).call() as PublishMethod
        }

        this.publishMethod as PublishMethod
    }

    /**
     * See: {@link GithubPublishSpec#setPublishMethod(PublishMethod)}
     */
    @Override
    GithubPublishSpec setPublishMethod(PublishMethod method) {
        this.publishMethod = method
        this
    }

    /**
     * See: {@link GithubPublishSpec#setPublishMethod(Object)}
     */
    @Override
    GithubPublishSpec setPublishMethod(Object method) {
        this.publishMethod = method
        this
    }

    /**
     * See: {@link GithubPublishSpec#setPublishMethod(PublishMethod)}
     */
    @Override
    GithubPublishSpec publishMethod(PublishMethod method) {
        this.setPublishMethod(method)
    }

    /**
     * See: {@link GithubPublishSpec#setPublishMethod(Object)}
     */
    @Override
    GithubPublishSpec publishMethod(Object method) {
        this.setPublishMethod(method)
    }

}
