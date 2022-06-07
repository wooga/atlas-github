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

package wooga.gradle.github.base.tasks.internal

import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import wooga.gradle.github.base.internal.GithubClientFactory
import wooga.gradle.github.base.internal.GithubRepositoryValidator
import wooga.gradle.github.base.GithubSpec

abstract class AbstractGithubTask<T extends AbstractGithubTask> extends ConventionTask implements GithubSpec {

    private final Class<T> taskType

    AbstractGithubTask(Class<T> taskType) {
        this.taskType = taskType
    }

    @Internal
    GHRepository getRepository() {
        GHRepository repository
        try {
            repository = client.getRepository(repositoryName.get())
        }
        catch (Exception error) {
            throw new GradleException("can't find repository ${repositoryName.get()}", error)
        }
        repository
    }

    @Internal
    GitHub getClient() {
        clientProvider.get()
    }
}
