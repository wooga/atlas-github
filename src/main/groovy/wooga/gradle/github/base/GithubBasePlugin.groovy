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

import org.ajoberstar.grgit.Grgit
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import wooga.gradle.github.base.internal.DefaultGithubPluginExtension
import wooga.gradle.github.base.internal.RepositoryInfo
import wooga.gradle.github.base.tasks.internal.AbstractGithubTask

import java.nio.file.Paths

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
        Provider<Grgit> git = project.provider { getsGitIfExists(project.rootProject.rootDir) }
        RepositoryInfo repoInfo = new RepositoryInfo(project, git)

        GithubPluginExtension extension = project.extensions.create(EXTENSION_NAME, DefaultGithubPluginExtension.class, project)

        extension.username.set(GithubBasePluginConvention.userName.getStringValueProvider(project))
        extension.password.set(GithubBasePluginConvention.password.getStringValueProvider(project))
        extension.token.set(GithubBasePluginConvention.token.getStringValueProvider(project))
        extension.baseUrl.set(GithubBasePluginConvention.baseUrl.getStringValueProvider(project))

        extension.repositoryName.set(GithubBasePluginConvention.repositoryName.getStringValueProvider(project, repoInfo.repositoryNameFromLocalGit))
        extension.branchName.set(GithubBasePluginConvention.branchName.getStringValueProvider(project, repoInfo.branchNameFromLocalGit))

        project.tasks.withType(AbstractGithubTask).configureEach { task ->
            task.baseUrl.set(extension.baseUrl)
            task.username.set(extension.username)
            task.password.set(extension.password)
            task.token.set(extension.token)
            task.repositoryName.set(extension.repositoryName)
            task.branchName.set(extension.branchName)
            //must be convention as the clientProvider set on task construction has priority
            task.clientProvider.convention(extension.clientProvider)
        }
    }

    static Grgit getsGitIfExists(File folder) {
        FileFilter filter = { File file -> file.directory && file.name == ".git"}
        def gitPresent = folder.listFiles(filter).size() > 0
        if(gitPresent) {
            return Grgit.init(dir: folder)
        }
        return null
    }
}
