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
import org.gradle.api.provider.Property
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
import wooga.gradle.github.publish.internal.*

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
 *     githubPublish {*         targetCommitish = "master"
 *         tagName = project.version
 *         releaseName = project.version
 *         body = "Release XYZ"
 *         prerelease = false
 *         draft = false
 *         publishMethod = "create"
 *         from(file('build/output'))
 *}*}
 */
class GithubPublish extends AbstractGithubTask implements GithubPublishSpec {

    private static final Logger logger = Logging.getLogger(GithubPublish)

    private File assetCollectDirectory
    private File assetUploadDirectory
    protected CopySpec assetsCopySpec
    private Boolean processAssets
    private Boolean isNewlyCreatedRelease = false

    @Input
    final Property<String> tagName

    @Input
    @Optional
    final Property<String> targetCommitish

    @Input
    final Property<String> releaseName

    @Input
    @Optional
    final Property<String> body

    @Input
    final Property<Boolean> prerelease

    @Input
    final Property<Boolean> draft

    final Property<PublishMethod> publishMethod

    @Override
    Property<Boolean> isPrerelease() {
        return prerelease
    }

    @Override
    Property<Boolean> isDraft() {
        return draft
    }

    GithubPublish() {
        super(GithubPublish.class)
        assetsCopySpec = project.copySpec()

        assetCollectDirectory = project.file("${temporaryDir}/collect")
        assetUploadDirectory = project.file("${temporaryDir}/prepare")

        tagName = project.objects.property(String)
        targetCommitish = project.objects.property(String)
        releaseName = project.objects.property(String)
        body = project.objects.property(String)

        prerelease = project.objects.property(Boolean)
        draft = project.objects.property(Boolean)
        publishMethod = project.objects.property(PublishMethod)
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
            GHRelease release = createOrUpdateGithubRelease(this.processAssets || draft.get())
            if (this.processAssets) {
                processReleaseAssets(release)
            }
        } catch (GithubReleasePublishException releaseError) {
            failRelease(releaseError.getRelease(), releaseError.message, isNewlyCreatedRelease, releaseError)
        }
        setDidWork(true)
    }

    protected void processReleaseAssets(GHRelease release) throws GithubReleaseUploadAssetsException, GithubReleaseUpdateException {
        getDestinationDir().mkdirs()
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
                if (release.draft != draft.get()) {
                    release.update().draft(draft.get()).tag(tagName.get()).update()
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
        GithubPublishRollbackHandler.rollback(release, deleteRelease, cause)
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

        assetUploadDirectory.listFiles().findAll { it.isFile() }.sort().each { File assetFile ->
            try {
                publishedAssets << ReleaseAssetUpload.uploadAsset(release, assetFile)
            } catch (HttpException httpError) {
                if (isDuplicateAssetError(httpError)) {
                    logger.info("asset ${assetFile.name} already published")
                    logger.info("attempt override")
                    def duplicateAsset = release.assets.find { it.name == assetFile.name }
                    if (duplicateAsset) {
                        try {
                            UpdatedAsset updatedAsset = UpdatedAsset.fromAsset(duplicateAsset)
                            duplicateAsset.delete()
                            updatedAssets << updatedAsset
                            publishedAssets << ReleaseAssetUpload.uploadAsset(release, assetFile)
                        } catch (Exception e) {
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
        GHRepository repository = getRepository()
        String tagName = tagName.get()
        GHRelease release = repository.listReleases().find({ it.tagName == tagName }) as GHRelease
        GithubReleasePropertySetter releasePropertySet
        GHRelease result
        if (release) {
            if (this.publishMethod.get() == PublishMethod.create) {
                throw new GithubReleaseCreateException("github release with tag ${tagName} already exist")
            }

            releasePropertySet = new GithubReleasePropertySetter(release.update())
            releasePropertySet.draft(draft.get())

            try {
                result = setReleasePropertiesAndCommit(releasePropertySet)
            } catch (Exception error) {
                throw new GithubReleaseUpdateException("failed to update release ${release.tagName}", error)
            }
        } else {
            if (this.publishMethod.get() == PublishMethod.update) {
                throw new GithubReleaseUpdateException("github release with tag ${tagName} for update not found")
            }

            isNewlyCreatedRelease = true
            releasePropertySet = new GithubReleasePropertySetter(repository.createRelease(tagName))
            releasePropertySet.draft(createDraft as boolean)

            try {
                result = setReleasePropertiesAndCommit(releasePropertySet)
            } catch (Exception error) {
                throw new GithubReleaseCreateException("failed to create release ${tagName}", error)
            }
        }
        result
    }

    protected GHRelease setReleasePropertiesAndCommit(GithubReleasePropertySetter releasePropertySet) {
        releasePropertySet.prerelease(prerelease.get())

        if (targetCommitish.present) {
            releasePropertySet.commitish(targetCommitish.get())
        }

        if (body.present) {
            releasePropertySet.body(body.get())
        }

        if (releaseName.present) {
            releasePropertySet.name(releaseName.get())
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

    /**
     * See: {@link GithubPublishSpec#setTagName(String)}
     */
    @Override
    GithubPublish setTagName(String tagName) {
        this.tagName.set(tagName)
        this
    }

    /**
     * See: {@link GithubPublishSpec#setTagName(Object)}
     */
    @Override
    GithubPublish setTagName(Object tagName) {
        this.tagName.set(project.provider({
            if (tagName instanceof Callable) {
                return ((Callable) tagName).call().toString()
            }

            tagName.toString()
        }))
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
     * See: {@link GithubPublishSpec#setTargetCommitish(String)}
     */
    @Override
    GithubPublish setTargetCommitish(String targetCommitish) {
        this.targetCommitish.set(targetCommitish)
        this
    }

    /**
     * See: {@link GithubPublishSpec#setTargetCommitish(Object)}
     */
    @Override
    GithubPublish setTargetCommitish(Object targetCommitish) {
        this.targetCommitish.set(project.provider({
            if (targetCommitish instanceof Callable) {
                return ((Callable) targetCommitish).call().toString()
            }

            targetCommitish.toString()
        }))
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
     * See: {@link GithubPublishSpec#setReleaseName(String)}
     */
    @Override
    GithubPublish setReleaseName(String name) {
        this.releaseName.set(name)
        this
    }

    /**
     * See: {@link GithubPublishSpec#setReleaseName(Object)}
     */
    @Override
    GithubPublish setReleaseName(Object name) {
        this.releaseName.set(project.provider({
            if (name instanceof Callable) {
                return ((Callable) name).call().toString()
            }

            name.toString()
        }))
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

//    /**
//     * See: {@link GithubPublishSpec#getBody()}
//     */
//    @Optional
//    @Input
//    @Override
//    String getBody() {
//        if (this.body == null) {
//            return null
//        }
//
//        if (this.body instanceof Closure) {
//            return ((Closure) this.body).call(getRepository(getClient())).toString()
//        }
//
//        if (this.body instanceof PublishBodyStrategy) {
//            return ((PublishBodyStrategy) this.body).getBody(getRepository(getClient()))
//        }
//
//        if (this.body instanceof Callable) {
//            return ((Callable) this.body).call().toString()
//        }
//
//        this.body.toString()
//    }

    /**
     * See: {@link GithubPublishSpec#setBody(String)}
     */
    @Override
    GithubPublish setBody(String body) {
        this.body.set(body)
        this
    }

    /**
     * See: {@link GithubPublishSpec#setBody(Object)}
     */
    @Override
    GithubPublish setBody(Object body) {
        this.body.set(project.provider({
            if (body instanceof Callable) {
                return ((Callable) body).call().toString()
            }

            body.toString()
        }))
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

        this.body.set(project.provider({
            closure.call(getRepository()).toString()
        }))
        this
    }

    /**
     * See: {@link GithubPublishSpec#setBody(PublishBodyStrategy)}
     */
    GithubPublish setBody(PublishBodyStrategy bodyStrategy) {
        this.body.set(project.provider({
            bodyStrategy.getBody(getRepository())
        }))
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
     * See: {@link GithubPublishSpec#setPrerelease(boolean)}
     */
    @Override
    GithubPublish setPrerelease(Boolean prerelease) {
        this.prerelease.set(prerelease)
        this
    }

    /**
     * See: {@link GithubPublishSpec#setPrerelease(Object)}
     */
    @Override
    GithubPublish setPrerelease(Object prerelease) {

        this.prerelease.set(project.provider({
            if (prerelease instanceof Callable) {
                return Boolean.parseBoolean(((Callable) prerelease).call().toString())
            }

            Boolean.parseBoolean(prerelease.toString())
        }))
        this
    }

    /**
     * See: {@link GithubPublishSpec#prerelease(boolean)}
     */
    @Override
    GithubPublish prerelease(Boolean prerelease) {
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
     * See: {@link GithubPublishSpec#setDraft(boolean)}
     */
    @Override
    GithubPublish setDraft(Boolean draft) {
        this.draft.set(draft)
        this
    }

    /**
     * See: {@link GithubPublishSpec#setDraft(Object)}
     */
    @Override
    GithubPublish setDraft(Object draft) {
        this.draft.set(project.provider({
            if (draft instanceof Callable) {
                return Boolean.parseBoolean(((Callable) draft).call().toString())
            }

            Boolean.parseBoolean(draft.toString())
        }))
        this
    }

    /**
     * See: {@link GithubPublishSpec#draft(boolean)}
     */
    @Override
    GithubPublish draft(Boolean draft) {
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
     * See: {@link GithubPublishSpec#setPublishMethod(PublishMethod)}
     */
    @Override
    GithubPublishSpec setPublishMethod(PublishMethod method) {
        this.publishMethod.set(method)
        this
    }

    /**
     * See: {@link GithubPublishSpec#setPublishMethod(Object)}
     */
    @Override
    GithubPublishSpec setPublishMethod(Object method) {
        this.publishMethod.set(project.provider({
            if (method instanceof Callable) {
                return ((Callable) method).call() as PublishMethod
            }

            method as PublishMethod
        }))
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
