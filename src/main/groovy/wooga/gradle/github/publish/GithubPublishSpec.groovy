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

package wooga.gradle.github.publish

import org.gradle.api.Task
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.util.PatternFilterable
import wooga.gradle.github.base.GithubSpec

/**
 * Task spec definition for a github publish task/action.
 * https://developer.github.com/v3/repos/releases/#create-a-release
 * <p>
 * All {@code set} methods come in two flavors.
 * <ul>
 *     <li> a set assignment method {@code setTagName(...)}
 *     <li> a mutator method {@code tagName(...)}
 *
 * Both return the current object.
 * The provided value can be any object with a valid conversion method available ({@code toString}, {@code toBoolean}, etc).
 * If the value is a {@code Closure} or {@code Callable} and the docs doesn't state otherwise,
 * the result after calling the object and executing the matching conversion method on the return value will be used.
 *
 * All values will be evaluated when calling the {@code get} method.
 *
 * <pre>
 * {@code
 *     String getReleaseName() {
 *         if(this.releaseName == null) {
 *             return null
 *         }
 *         if (this.releaseName instanceof Callable) {
 *             return ((Callable) this.releaseName).call().toString()
 *         }
 *         this.releaseName.toString()
 *     }
 * }
 */
trait GithubPublishSpec extends GithubSpec implements CopySourceSpec, PatternFilterable {

    private final Property<String> tagName = objects.property(String)

    /**
     * Returns the name of the Git tag from which to create the release.
     *
     * @return the Git tag name
     */
    @Input
    Property<String> getTagName() {
        tagName
    }


    /**
     * Sets the tag name for the release.
     *
     * @param the {@code String} tagName value
     */
    void setTagName(Provider<String> value) {
        tagName.set(value)
    }

    private final Property<String> targetCommitish = objects.property(String)

    /**
     * Returns the commitish value that determines where the Git tag is created from.
     * <p>
     * Can be any branch or commit SHA. Unused if the Git tag already exists.
     *
     * @default the repository's default branch (usually master).
     * @return this
     */
    @Optional
    @Input
    Property<String> getTargetCommitish() {
        targetCommitish
    }

    /**
     * Sets the commitish value.
     *
     * @param targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    void setTargetCommitish(Provider<String> value) {
        targetCommitish.set(value)
    }

    private final Property<String> releaseName = objects.property(String)

    /**
     * Returns the name of the release.
     *
     * @return the name of the release
     */
    @Input
    Property<String> getReleaseName() {
        releaseName
    }

    /**
     * Sets the name of the release.
     *
     * @param name the name to use for the release
     * @return this
     */
    void setReleaseName(Provider<String> value) {
        releaseName.set(value)
    }

    private final Property<String> body = objects.property(String)

    /**
     * Returns the description text of the release
     *
     * @return the release description
     */
    @Optional
    @Input
    Property<String> getBody() {
        body
    }

    /**
     * Sets the description text for the release.
     *
     * @param body the release description
     * @return this
     */
    void setBody(Provider<String> value) {
        body.set(value)
    }

    private final Property<Boolean> prerelease = objects.property(Boolean)

    /**
     * Returns a {@code boolean} value indicating if the release as a prerelease.
     *
     * @return {@code true} to identify the release as a prerelease. {@code false} to identify the release as a full release.
     * @default {@code false}
     */
    @Input
    Property<Boolean> getPrerelease() {
        prerelease
    }

    /**
     * Sets the prerelease status of the release.
     *
     * @param prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    void setPrerelease(Provider<Boolean> value) {
        prerelease.set(value)
    }

    private final Property<Boolean> draft = objects.property(Boolean)

    /**
     * Returns a {@code boolean} value indicating if the release will be automatically published.
     *
     * @return {@code true} to create a draft (unpublished) release, {@code false} to create a published one.
     * @default {@code false}
     */
    @Input
    Property<Boolean> getDraft() {
        draft
    }

    /**
     * Sets the publication status for the release.
     *
     * @param draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    void setDraft(Provider<Boolean> value) {
        draft.set(value)
    }

    private final Property<PublishMethod> publishMethod = objects.property(PublishMethod)

    /**
     * Returns a {@code PublishMethod} value indicating if a release should be created or updated.
     *
     * Potential values are
     * * create
     * * update
     * * createOrUpdate
     *
     * This method parameter can be used to specify the intention of the publish operation. The {@code GithubPublish}
     * will use the given @{code tagName} to find a release in the repository.
     * When set to {@code create}, the {@code GithubPublish} task will fail if a release already exists.
     * When set to {@code update}, the {@code GithubPublish} task will fail if a release doesn't exists.
     * When set to {@code createOrUpdate}, the {@code GithubPublish} task will create a release if missing.
     *
     * @return {@code PublishMethod} indicating if a release should be created or updated.
     * @default {@code PublishMethod.create}
     * @see wooga.gradle.github.publish.tasks.GithubPublish* @see wooga.gradle.github.publish.PublishMethod
     */
    @Input
    Property<PublishMethod> getPublishMethod() {
        publishMethod
    }

    /**
     * Sets the publish method.
     *
     * @param {@code PublishMethod} indicating if a release should be created or updated.
     * @return this*
     * @see #getPublishMethod()
     */
    void setPublishMethod(Provider<PublishMethod> value) {
        publishMethod.set(value)
    }

    void setPublishMethod(String value) {
        publishMethod.set(PublishMethod.valueOf(value))
    }


}
