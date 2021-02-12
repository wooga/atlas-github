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

package wooga.gradle.github.publish

import org.gradle.api.Task
import org.gradle.api.file.CopySourceSpec
import org.gradle.api.provider.Property
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
interface GithubPublishSpec extends GithubSpec, CopySourceSpec, PatternFilterable {

    /**
     * Returns the name of the Git tag from which to create the release.
     *
     * @return the Git tag name
     */
    Property<String> getTagName()

    /**
     * Sets the tag name for the release.
     *
     * @param the {@code String} tagName value
     * @return this
     */
    GithubPublishSpec setTagName(String tagName)

    /**
     * Sets the tag name for the release.
     *
     * @param the {@code Object} tagName value.
     * @return this
     */
    GithubPublishSpec setTagName(Object tagName)

    /**
     * Sets the tag name for the release.
     *
     * @param the {@code String} tagName value
     * @return this
     */
    GithubPublishSpec tagName(String tagName)

    /**
     * Sets the tag name for the release.
     *
     * @param the {@code Object} tagName value.
     * @return this
     */
    GithubPublishSpec tagName(Object tagName)

    /**
     * Returns the commitish value that determines where the Git tag is created from.
     * <p>
     * Can be any branch or commit SHA. Unused if the Git tag already exists.
     *
     * @default the repository's default branch (usually master).
     * @return this
     */
    Property<String> getTargetCommitish()

    /**
     * Sets the commitish value.
     *
     * @param targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    GithubPublishSpec setTargetCommitish(String targetCommitish)

    /**
     * Sets the commitish value.
     *
     * @param targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    GithubPublishSpec setTargetCommitish(Object targetCommitish)

    /**
     * Sets the commitish value.
     *
     * @param targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    GithubPublishSpec targetCommitish(String targetCommitish)

    /**
     * Sets the commitish value.
     *
     * @param targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    GithubPublishSpec targetCommitish(Object targetCommitish)

    /**
     * Returns the name of the release.
     *
     * @return the name of the release
     */
    Property<String> getReleaseName()

    /**
     * Sets the name of the release.
     *
     * @param name the name to use for the release
     * @return this
     */
    GithubPublishSpec setReleaseName(String name)

    /**
     * Sets the name of the release.
     *
     * @param name the name to use for the release
     * @return this
     */
    GithubPublishSpec setReleaseName(Object name)

    /**
     * Sets the name of the release.
     *
     * @param name the name to use for the release
     * @return this
     */
    GithubPublishSpec releaseName(String name)

    /**
     * Sets the name of the release.
     *
     * @param name the name to use for the release
     * @return this
     */
    GithubPublishSpec releaseName(Object name)

    /**
     * Returns the description text of the release
     *
     * @return the release description
     */
    Property<String> getBody()

    /**
     * Sets the description text for the release.
     *
     * @param body the release description
     * @return this
     */
    GithubPublishSpec setBody(String body)

    /**
     * Sets the description text for the release.
     * <p>
     * The provided value can be any object with a valid {@code toString} method.
     * If the value is a {@code Callable}, the result after calling the object
     * and executing {@code toString} on the return value will be used.
     *
     * @param body the release description
     * @return this
     */
    GithubPublishSpec setBody(Object body)

    /**
     * Sets the description text for the release.
     * <p>
     * The closure will be called with a {@link org.kohsuke.github.GHRepository} repository object.
     * The repository object can be used to query the Git commit log or pull requests etc.
     *
     * @param closure a configuration closure which returns the release description
     * @return this
     */
    GithubPublishSpec setBody(Closure closure)

    /**
     * Sets the description text for the release.
     * <p>
     * The body strategies {@code getBody( )} method will be called with a {@link org.kohsuke.github.GHRepository} repository object.
     * The repository object can be used to query the Git commit log or pull requests etc.
     *
     * @param bodyStrategy an object of type {@link PublishBodyStrategy} which returns the release description
     * @return this
     * @see PublishBodyStrategy#getBody(org.kohsuke.github.GHRepository)
     */
    GithubPublishSpec setBody(PublishBodyStrategy bodyStrategy)

    /**
     * Sets the description text for the release.
     * <p>
     * This setter allows to set a {@code File} property as the provider for the release body property.
     * The content of the file is read at runtime of the task.
     *
     * @param body a {@code File} which contains the body text
     * @return this
     */
    GithubPublishSpec setBody(File body)

    /**
     * Sets the description text for the release.
     * <p>
     * This setter allows to set a {@code Task} property as the provider for the release body property.
     * The task outputs will be evaluated at runtime. The task should only produce a single output file which
     * will be read and set as the release body value.
     * <p>
     * If the task produces no outputs or more than one, the publish task will fail.
     *
     * @param body a {@code File} which contains the body text
     * @return this
     */
    GithubPublishSpec setBody(Task body)

    /**
     * Sets the description text for the release.
     *
     * @param body the release description
     * @return this
     */
    GithubPublishSpec body(String body)

    /**
     * Sets the description text for the release.
     *
     * @param body the release description
     * @return this
     */
    GithubPublishSpec body(Object body)

    /**
     * Sets the description text for the release.
     * <p>
     * The closure will be called with a {@link org.kohsuke.github.GHRepository} repository object.
     * The repository object can be used to query the Git commit log or pull requests etc.
     *
     * @param closure a configuration closure which returns the release description
     * @return this
     */
    GithubPublishSpec body(Closure bodyStrategy)

    /**
     * Sets the description text for the release.
     * <p>
     * The body strategies {@code getBody( )} method will be called with a {@link org.kohsuke.github.GHRepository} repository object.
     * The repository object can be used to query the Git commit log or pull requests etc.
     *
     * @param bodyStrategy an object of type {@link PublishBodyStrategy} which returns the release description
     * @return this* @see PublishBodyStrategy#getBody(org.kohsuke.github.GHRepository)
     */
    GithubPublishSpec body(PublishBodyStrategy bodyStrategy)

    /**
     * Sets the description text for the release.
     * <p>
     * This setter allows to set a {@code File} property as the provider for the release body property.
     * The content of the file is read at runtime of the task.
     *
     * @param body a {@code File} which contains the body text
     * @return this
     */
    GithubPublishSpec body(File body)

    /**
     * Sets the description text for the release.
     * <p>
     * This setter allows to set a {@code Task} property as the provider for the release body property.
     * The task outputs will be evaluated at runtime. The task should only produce a single output file which
     * will be read and set as the release body value.
     * <p>
     * If the task produces no outputs or more than one, the publish task will fail.
     *
     * @param body a {@code File} which contains the body text
     * @return this
     */
    GithubPublishSpec body(Task body)

    /**
     * Returns a {@code boolean} value indicating if the release as a prerelease.
     *
     * @return {@code true} to identify the release as a prerelease. {@code false} to identify the release as a full release.
     * @default {@code false}
     */
    Property<Boolean> isPrerelease()

    /**
     * Sets the prerelease status of the release.
     *
     * @param prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    GithubPublishSpec setPrerelease(Boolean prerelease)

    /**
     * Sets the prerelease status of the release.
     *
     * @param prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    GithubPublishSpec setPrerelease(Object prerelease)

    /**
     * Sets the prerelease status of the release.
     *
     * @param prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    GithubPublishSpec prerelease(Boolean prerelease)

    /**
     * Sets the prerelease status of the release.
     *
     * @param prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    GithubPublishSpec prerelease(Object prerelease)

    /**
     * Returns a {@code boolean} value indicating if the release will be automatically published.
     *
     * @return {@code true} to create a draft (unpublished) release, {@code false} to create a published one.
     * @default {@code false}
     */
    Property<Boolean> isDraft()

    /**
     * Sets the publication status for the release.
     *
     * @param draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    GithubPublishSpec setDraft(Boolean draft)

    /**
     * Sets the publication status for the release.
     *
     * @param draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    GithubPublishSpec setDraft(Object draft)

    /**
     * Sets the publication status for the release.
     *
     * @param draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    GithubPublishSpec draft(Boolean draft)

    /**
     * Sets the publication status for the release.
     *
     * @param draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    GithubPublishSpec draft(Object draft)

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
    Property<PublishMethod> getPublishMethod()

    /**
     * Sets the publish method.
     *
     * @param {@code PublishMethod} indicating if a release should be created or updated.
     * @return this*
     * @see #getPublishMethod()
     */
    GithubPublishSpec setPublishMethod(PublishMethod method)

    /**
     * Sets the publish method.
     *
     * @param {@code Object} which can be converted to a {@code PublishMethod}
     * @return this*
     * @see #getPublishMethod()
     */
    GithubPublishSpec setPublishMethod(Object method)

    /**
     * Sets the publish method.
     *
     * @param {@code PublishMethod} indicating if a release should be created or updated.
     * @return this*
     * @see #getPublishMethod()
     */
    GithubPublishSpec publishMethod(PublishMethod method)

    /**
     * Sets the publish method.
     *
     * @param {@code Object} which can be converted to a {@code PublishMethod}
     * @return this*
     * @see #getPublishMethod()
     */
    GithubPublishSpec publishMethod(Object method)
}
