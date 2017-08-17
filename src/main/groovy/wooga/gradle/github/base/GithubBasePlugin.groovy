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

package wooga.gradle.github.base

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.TaskContainer

class GithubBasePlugin implements Plugin<Project> {

    static final String EXTENSION_NAME = "github"
    static final String GROUP = "github"

    private Project project
    private TaskContainer tasks

    @Override
    void apply(Project project) {
        this.project = project
        this.tasks = project.tasks

        GithubPluginExtention extension = project.extensions.create(EXTENSION_NAME, DefaultGithubPluginExtention.class, project.rootProject.properties)
        configureAbstractGithubTaskDefaults(extension)
    }

    private void configureAbstractGithubTaskDefaults(GithubPluginExtention extention) {
        tasks.withType(AbstractGithubTask, new Action<AbstractGithubTask>() {
            @Override
            void execute(AbstractGithubTask task) {
                ConventionMapping taskConventionMapping = task.getConventionMapping()

                taskConventionMapping.map("baseUrl", { extention.getBaseUrl() })
                taskConventionMapping.map("repository", { extention.getRepository() })
                taskConventionMapping.map("userName", { extention.getUserName() })
                taskConventionMapping.map("password", { extention.getPassword() })
                taskConventionMapping.map("token", { extention.getToken() })

                task.onlyIf(new Spec<Task>() {
                    @Override
                    boolean isSatisfiedBy(Task t) {
                        AbstractGithubTask publishTask = (AbstractGithubTask) t
                        return publishTask.repository != null
                    }
                })
            }
        })
    }
}
