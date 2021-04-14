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

package wooga.gradle.github.base

import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
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
        GithubPluginExtension extension = project.extensions.create(EXTENSION_NAME, DefaultGithubPluginExtension.class, project)

        extension.username.set(project.provider({
            def value = project.properties.get(GithubBasePluginConvention.GITHUB_USER_NAME_OPTION)
            if (value) {
                return value.toString()
            }
            null
        }))

        extension.password.set(project.provider({
            def value = project.properties.get(GithubConsts.GITHUB_USER_PASSWORD_OPTION)
            if (value) {
                return value.toString()
            }
            null
        }))

        extension.token.set(project.provider({
            def value = project.properties.get(GithubBasePluginConvention.GITHUB_TOKEN_OPTION)
            if (value) {
                return value.toString()
            }
            null
        }))

        extension.repositoryName.set(project.provider({
            def value = project.properties.get(GithubBasePluginConvention.GITHUB_REPOSITORY_NAME_OPTION)
            if (value) {
                return value.toString()
            }
            null
        }))

        project.tasks.withType(AbstractGithubTask, new Action<AbstractGithubTask>() {
            @Override
            void execute(AbstractGithubTask task) {
                task.baseUrl.set(extension.baseUrl)
                task.repositoryName.set(extension.repositoryName)
                task.username.set(extension.username)
                task.password.set(extension.password)
                task.token.set(extension.token)
            }
        })
    }
}
