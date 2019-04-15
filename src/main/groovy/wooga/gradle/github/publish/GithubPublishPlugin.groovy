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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.github.base.GithubBasePlugin
import wooga.gradle.github.publish.tasks.GithubPublish

/**
 * A {@link org.gradle.api.Plugin} which provides tasks and conventions to publish artifacts to github.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     plugins{
 *         id "net.wooga.github" version "0.6.1"
 *     }
 *
 *     github {
 *         username = "wooga"
 *         password = "the_password"
 *         token "a github access token"
 *         repositoryName "wooga/atlas-github"
 *         baseUrl = null
 *     }
 *
 *     githubPublish {
 *         targetCommitish = "master
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
        def githubPublish = tasks.create(name: PUBLISH_TASK_NAME, type: GithubPublish, group: GithubBasePlugin.GROUP)
        githubPublish.description = "Publish github release"
        tasks.getByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME).dependsOn githubPublish
    }

    private static void configurePublishTaskDefaults(final Project project) {
        project.tasks.withType(GithubPublish, new Action<GithubPublish>() {
            @Override
            void execute(GithubPublish task) {
                task.targetCommitish.set("master")
                task.prerelease.set(false)
                task.draft.set(false)
                task.publishMethod.set(PublishMethod.create)

                def projectProvider = project.provider({project.version.toString()})

                task.tagName.set(projectProvider)
                task.releaseName.set(projectProvider)

                task.onlyIf(new Spec<GithubPublish>() {
                    @Override
                    boolean isSatisfiedBy(GithubPublish publishTask) {
                        publishTask.repositoryName.present
                    }
                })
            }
        })
    }
}
