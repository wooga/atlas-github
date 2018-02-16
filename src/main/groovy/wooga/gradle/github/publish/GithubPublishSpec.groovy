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

import org.gradle.api.file.CopySourceSpec
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
    String getTagName()

    /**
     * Sets the tag name for the release.
     *
     * @param  the {@code String} tagName value
     * @return this
     */
    GithubPublishSpec setTagName(String tagName)

    /**
     * Sets the tag name for the release.
     *
     * @param  the {@code Object} tagName value.
     * @return this
     */
    GithubPublishSpec setTagName(Object tagName)

    /**
     * Sets the tag name for the release.
     *
     * @param  the {@code String} tagName value
     * @return this
     */
    GithubPublishSpec tagName(String tagName)

    /**
     * Sets the tag name for the release.
     *
     * @param  the {@code Object} tagName value.
     * @return this
     */
    GithubPublishSpec tagName(Object tagName)

    /**
     * Returns the commitish value that determines where the Git tag is created from.
     * <p>
     * Can be any branch or commit SHA. Unused if the Git tag already exists.
     *
     * @default the repository's default branch (usually master).
     * @return  this
     */
    String getTargetCommitish()

    /**
     * Sets the commitish value.
     *
     * @param  targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    GithubPublishSpec setTargetCommitish(String targetCommitish)

    /**
     * Sets the commitish value.
     *
     * @param  targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    GithubPublishSpec setTargetCommitish(Object targetCommitish)

    /**
     * Sets the commitish value.
     *
     * @param  targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    GithubPublishSpec targetCommitish(String targetCommitish)

    /**
     * Sets the commitish value.
     *
     * @param  targetCommitish the commitish value, can be any branch or commit SHA
     * @return this
     */
    GithubPublishSpec targetCommitish(Object targetCommitish)

    /**
     * Returns the name of the release.
     *
     * @return the name of the release
     */
    String getReleaseName()

    /**
     * Sets the name of the release.
     *
     * @param  name the name to use for the release
     * @return this
     */
    GithubPublishSpec setReleaseName(String name)

    /**
     * Sets the name of the release.
     *
     * @param  name the name to use for the release
     * @return this
     */
    GithubPublishSpec setReleaseName(Object name)

    /**
     * Sets the name of the release.
     *
     * @param  name the name to use for the release
     * @return this
     */
    GithubPublishSpec releaseName(String name)

    /**
     * Sets the name of the release.
     *
     * @param  name the name to use for the release
     * @return this
     */
    GithubPublishSpec releaseName(Object name)

    /**
     * Returns the description text of the release
     *
     * @return the release description
     */
    String getBody()

    /**
     * Sets the description text for the release.
     *
     * @param  body the release description
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
     * @param  body the release description
     * @return this
     */
    GithubPublishSpec setBody(Object body)

    /**
     * Sets the description text for the release.
     * <p>
     * The closure will be called with a {@link org.kohsuke.github.GHRepository} repository object.
     * The repository object can be used to query the Git commit log or pull requests etc.
     *
     * @param  closure a configuration closure which returns the release description
     * @return this
     */
    GithubPublishSpec setBody(Closure closure)

    /**
     * Sets the description text for the release.
     * <p>
     * The body strategies {@code getBody()} method will be called with a {@link org.kohsuke.github.GHRepository} repository object.
     * The repository object can be used to query the Git commit log or pull requests etc.
     *
     * @param  bodyStrategy an object of type {@link PublishBodyStrategy} which returns the release description
     * @return this
     * @see    PublishBodyStrategy#getBody(org.kohsuke.github.GHRepository)
     */
    GithubPublishSpec setBody(PublishBodyStrategy bodyStrategy)

    /**
     * Sets the description text for the release.
     *
     * @param  body the release description
     * @return this
     */
    GithubPublishSpec body(String body)

    /**
     * Sets the description text for the release.
     *
     * @param  body the release description
     * @return this
     */
    GithubPublishSpec body(Object body)

    /**
     * Sets the description text for the release.
     * <p>
     * The closure will be called with a {@link org.kohsuke.github.GHRepository} repository object.
     * The repository object can be used to query the Git commit log or pull requests etc.
     *
     * @param  closure a configuration closure which returns the release description
     * @return this
     */
    GithubPublishSpec body(Closure bodyStrategy)

    /**
     * Sets the description text for the release.
     * <p>
     * The body strategies {@code getBody()} method will be called with a {@link org.kohsuke.github.GHRepository} repository object.
     * The repository object can be used to query the Git commit log or pull requests etc.
     *
     * @param  bodyStrategy an object of type {@link PublishBodyStrategy} which returns the release description
     * @return this
     * @see    PublishBodyStrategy#getBody(org.kohsuke.github.GHRepository)
     */
    GithubPublishSpec body(PublishBodyStrategy bodyStrategy)

    /**
     * Returns a {@code boolean} value indicating if the release as a prerelease.
     *
     * @return  {@code true} to identify the release as a prerelease. {@code false} to identify the release as a full release.
     * @default {@code false}
     */
    boolean isPrerelease()

    /**
     * Sets the prerelease status of the release.
     *
     * @param  prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    GithubPublishSpec setPrerelease(boolean prerelease)

    /**
     * Sets the prerelease status of the release.
     *
     * @param  prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    GithubPublishSpec setPrerelease(Object prerelease)

    /**
     * Sets the prerelease status of the release.
     *
     * @param  prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    GithubPublishSpec prerelease(boolean prerelease)

    /**
     * Sets the prerelease status of the release.
     *
     * @param  prerelease the prerelease status. Set {@code true} to identify the release as a prerelease.
     * @return this
     */
    GithubPublishSpec prerelease(Object prerelease)

    /**
     * Returns a {@code boolean} value indicating if the release will be automatically published.
     *
     * @return  {@code true} to create a draft (unpublished) release, {@code false} to create a published one.
     * @default {@code false}
     */
    boolean isDraft()

    /**
     * Sets the publication status for the release.
     *
     * @param  draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    GithubPublishSpec setDraft(boolean draft)

    /**
     * Sets the publication status for the release.
     *
     * @param  draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    GithubPublishSpec setDraft(Object draft)

    /**
     * Sets the publication status for the release.
     *
     * @param  draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    GithubPublishSpec draft(boolean draft)

    /**
     * Sets the publication status for the release.
     *
     * @param  draft the status. Set to {@code true} to create a draft (unpublished) release.
     * @return this
     */
    GithubPublishSpec draft(Object draft)
}