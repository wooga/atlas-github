/*
 * Copyright 2019-2021 Wooga GmbH
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

package wooga.gradle.github.publish.internal


import org.kohsuke.github.GHRelease
import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GHReleaseUpdater

/**
 * A utility wrapper type.
 *
 * This object is a wrapper around either {@code GHReleaseBuilder} or {@code GHReleaseUpdater} objects.
 * It allows to pass objects of these types to be handled with a common interface.
 */
class GithubReleasePropertySetter {

    private final GHReleaseUpdater updater
    private final GHReleaseBuilder builder

    GithubReleasePropertySetter(GHReleaseUpdater updater) {
        this.updater = updater
        this.builder = null
    }

    GithubReleasePropertySetter(GHReleaseBuilder builder) {
        this.builder = builder
        this.updater = null
    }

    /**
     * @param body The release notes body.
     */
    GithubReleasePropertySetter body(String body) {
        if(updater) {
            updater.body(body)
        }

        if(builder) {
            builder.body(body)
        }

        this
    }

    /**
     * Specifies the commitish value that determines where the Git tag is created from. Can be any branch or
     * commit SHA.
     *
     * @param commitish Defaults to the repository’s default branch (usually "master"). Unused if the Git tag
     *                  already exists.
     */
    GithubReleasePropertySetter commitish(String commitish) {
        if(updater) {
            updater.commitish(commitish)
        }

        if(builder) {
            builder.commitish(commitish)
        }

        this
    }

    /**
     * Optional.
     *
     * @param draft {@code true} to create a draft (unpublished) release, {@code false} to create a published one.
     *                          Default is {@code false}.
     */
    GithubReleasePropertySetter draft(boolean draft) {
        if(updater) {
            updater.draft(draft)
        }

        if(builder) {
            builder.draft(draft)
        }
        this
    }

    /**
     * @param name the name of the release
     */
    GithubReleasePropertySetter name(String name) {
        if(updater) {
            updater.name(name)
        }

        if(builder) {
            builder.name(name)
        }
        this
    }

    /**
     * Optional
     *
     * @param prerelease {@code true} to identify the release as a prerelease. {@code false} to identify the release
     *                               as a full release. Default is {@code false}.
     */
    GithubReleasePropertySetter prerelease(boolean prerelease) {
        if(updater) {
            updater.prerelease(prerelease)
        }

        if(builder) {
            builder.prerelease(prerelease)
        }
        this
    }

    /**
     * Executes the builders native commit method.
     *
     * If the wrapped object is a {@code GHReleaseUpdater}, calls {@code update}.
     * If the wrapped object is a {@code GHReleaseBuilder}, calls {@code create}.
     *
     * @return the created or updated {@code GHRelease} object
     * @throws IOException
     */
    GHRelease commit() throws IOException {
        if(updater) {
            return updater.update()
        }

        if(builder) {
            return builder.create()
        }
        return null
    }
}
