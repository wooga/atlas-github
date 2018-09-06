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

import org.gradle.api.Plugin
import org.gradle.api.Project
import wooga.gradle.github.publish.GithubPublishPlugin

/**
 * A {@link org.gradle.api.Plugin} which provides tasks and conventions to publish artifacts to github.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     plugins {
 *         id "net.wooga.github" version "0.6.1"
 *     }
 *
 *     github {
 *         username = "wooga"
 *         password = "the-password"
 *         token "a github access token"
 *         repositoryName "wooga/atlas-github"
 *         baseUrl = null
 *     }
 *
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
 * </pre>
 */
class GithubPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.pluginManager.apply(GithubPublishPlugin)
    }
}
