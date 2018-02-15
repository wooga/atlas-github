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
import org.gradle.api.internal.ConventionMapping
import wooga.gradle.github.base.internal.DefaultGithubPluginExtension
import wooga.gradle.github.base.tasks.internal.AbstractGithubTask

/**
 * A base {@link org.gradle.api.Plugin} to register and set conventions for all {@link AbstractGithubTask} types.
 *
 */
class GithubBasePlugin implements Plugin<Project> {

    /**
     * Value for github gradle convention.
     * @value "github"
     */
    static final String EXTENSION_NAME = "github"

    /**
     * Value for github gradle task group.
     * @value "github"
     */
    static final String GROUP = "github"

    @Override
    void apply(Project project) {
        def tasks = project.tasks

        GithubPluginExtention extension = project.extensions.create(EXTENSION_NAME, DefaultGithubPluginExtension.class, project.rootProject.properties)
        tasks.withType(AbstractGithubTask, new Action<AbstractGithubTask>() {
            @Override
            void execute(AbstractGithubTask task) {
                ConventionMapping taskConventionMapping = task.getConventionMapping()
                taskConventionMapping.map("baseUrl", { extension.getBaseUrl() })
                taskConventionMapping.map("repositoryName", { extension.getRepositoryName() })
                taskConventionMapping.map("username", { extension.getUsername() })
                taskConventionMapping.map("password", { extension.getPassword() })
                taskConventionMapping.map("token", { extension.getToken() })
            }
        })
    }
}
