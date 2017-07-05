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

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskContainer
import wooga.gradle.github.base.GithubBasePlugin
import wooga.gradle.github.base.GithubPluginExtention

class GithubPublishPlugin implements Plugin<Project> {

    static final String PUBLISH_TASK_NAME = "githubPublish"

    private Project project
    private TaskContainer tasks

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        project.pluginManager.apply(GithubBasePlugin)
        project.pluginManager.apply(PublishingPlugin)
        GithubPluginExtention extension = (GithubPluginExtention) project.extensions.getByName(GithubBasePlugin.EXTENSION_NAME)

        createDefaultPublishTask()
        configurePublishTaskDefaults(extension)
    }

    private void createDefaultPublishTask() {
        def githubPublish = tasks.create(name: PUBLISH_TASK_NAME, type: GithubPublish, group: GithubBasePlugin.GROUP)
        githubPublish.description = "Publish artifacts to github releases"
        tasks.getByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME).dependsOn githubPublish
    }

    private void configurePublishTaskDefaults(GithubPluginExtention extention) {
        tasks.withType(GithubPublish, new Action<GithubPublish>() {
            @Override
            void execute(GithubPublish task) {
                ConventionMapping taskConventionMapping = task.getConventionMapping()

                taskConventionMapping.map("baseUrl", { extention.getBaseUrl() })
                taskConventionMapping.map("repository", { extention.getRepository() })
                taskConventionMapping.map("userName", { extention.getUserName() })
                taskConventionMapping.map("password", { extention.getPassword() })
                taskConventionMapping.map("token", { extention.getToken() })
                taskConventionMapping.map("targetCommitish", { "master" })
                taskConventionMapping.map("prerelease", { false })
                taskConventionMapping.map("draft", { false })
                taskConventionMapping.map("tagName", { project.version.toString() })
                taskConventionMapping.map("releaseName", { project.version.toString() })

                task.onlyIf(new Spec<Task>() {
                    @Override
                    boolean isSatisfiedBy(Task t) {
                        GithubPublish publishTask = (GithubPublish) t
                        return publishTask.repository != null
                    }
                })
            }
        })
    }
}
