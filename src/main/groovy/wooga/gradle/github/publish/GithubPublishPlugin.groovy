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

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import wooga.gradle.github.base.GithubBasePlugin
import wooga.gradle.github.publish.tasks.GithubPublish

/**
 * A {@link org.gradle.api.Plugin} which provides tasks and conventions to publish artifacts to github.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     plugins{*         id "net.wooga.github" version "0.6.1"
 *}*
 *     github {*         username = "wooga"
 *         password = "the_password"
 *         token "a github access token"
 *         repositoryName "wooga/atlas-github"
 *         baseUrl = null
 *}*
 *     githubPublish {*         targetCommitish = "master
 *         tagName = project.version
 *         releaseName = project.version
 *         body = "Release XYZ"
 *         prerelease = false
 *         draft = false
 *
 *         from(file('build/output'))
 *}*}
 * </pre>
 */
class GithubPublishPlugin implements Plugin<Project> {

    /**
     * Value for github publish task.
     * @value "githubPublish"
     */
    static final String PUBLISH_TASK_NAME = "githubPublish"

    @Override
    void apply(Project project) {
        def tasks = project.tasks

        project.pluginManager.apply(GithubBasePlugin)
        project.pluginManager.apply(PublishingPlugin)

        createDefaultPublishTask(tasks)
        configurePublishTaskDefaults(project)
    }

    private static void createDefaultPublishTask(final TaskContainer tasks) {
        def githubPublish = tasks.register(PUBLISH_TASK_NAME, GithubPublish)
        githubPublish.configure { GithubPublish task ->
            task.group = GithubBasePlugin.GROUP
            task.description = "Publish github release"
        }
        tasks.named(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME).configure { task ->
            task.dependsOn(githubPublish)
        }
    }

    private static void configurePublishTaskDefaults(final Project project) {
        def projectProvider = project.provider({ project.version.toString() })
        project.tasks.withType(GithubPublish).configureEach {task ->
            task.prerelease.set(false)
            task.draft.set(false)
            task.publishMethod.set(PublishMethod.create)

            task.tagName.set(projectProvider)
            task.releaseName.set(projectProvider)

            task.onlyIf { GithubPublish publishTask -> publishTask.repositoryName.present }
        }
    }
}
