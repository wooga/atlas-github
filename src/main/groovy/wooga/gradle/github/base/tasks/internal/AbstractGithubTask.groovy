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

package wooga.gradle.github.base.tasks.internal

import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.kohsuke.github.HttpException
import wooga.gradle.github.base.internal.GithubRepositoryValidator
import wooga.gradle.github.base.GithubSpec
import wooga.gradle.github.base.tasks.Github

abstract class AbstractGithubTask<T extends AbstractGithubTask> extends ConventionTask implements GithubSpec {

    @Input
    final Property<String> repositoryName

    @Optional
    @Input
    final Property<String> baseUrl

    @Optional
    @Input
    final Property<String> username

    @Optional
    @Input
    final Property<String> password


    @Optional
    @Input
    final Property<String> token

    protected final Property<GitHub> clientProvider

    private final Class<T> taskType

    AbstractGithubTask(Class<T> taskType) {
        this.taskType = taskType

        repositoryName = project.objects.property(String)
        baseUrl = project.objects.property(String)
        username = project.objects.property(String)
        password = project.objects.property(String)
        token = project.objects.property(String)

        clientProvider = project.objects.property(GitHub)

        clientProvider.set(project.provider({
            def builder = new GitHubBuilder()

            if (username.present && password.present) {
                builder = builder.withPassword(username.get(), password.get())
            } else if (username.present && token.present) {
                builder = builder.withOAuthToken(token.get(), username.get())

            } else if (token.present) {
                builder = builder.withOAuthToken(token.get())

            } else {
                builder = GitHubBuilder.fromCredentials()
            }

            if (baseUrl.present) {
                builder = builder.withEndpoint(baseUrl.get())
            }

            def client = builder.build()
            clientProvider.set(client)
            return client
        }))
    }

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

    GitHub getClient() {
        this.clientProvider.get()
    }

    @Override
    T setRepositoryName(String name) {
        if (!GithubRepositoryValidator.validateRepositoryName(name)) {
            throw new IllegalArgumentException("Repository value '$name' is not a valid github repository name. Expecting `owner/repo`.")
        }

        this.repositoryName.set(name)
        taskType.cast(this)
    }

    @Override
    T repositoryName(String name) {
        taskType.cast(this.setRepositoryName(name))
    }

    @Override
    T setBaseUrl(String baseUrl) {
        this.baseUrl.set(baseUrl)
        taskType.cast(this)
    }

    @Override
    T baseUrl(String baseUrl) {
        taskType.cast(this.setBaseUrl(baseUrl))
    }

    @Override
    T setToken(String token) {
        this.token.set(token)
        taskType.cast(this)
    }

    @Override
    T token(String token) {
        taskType.cast(this.setToken(token))
    }

    @Override
    T setUsername(String userName) {
        this.username.set(userName)
        taskType.cast(this)
    }

    @Override
    T username(String username) {
        this.setUsername(username)
        taskType.cast(this)
    }

    @Override
    T setPassword(String password) {
        this.password.set(password)
        taskType.cast(this)
    }

    @Override
    T password(String password) {
        this.setPassword(password)
        taskType.cast(this)
    }
}
