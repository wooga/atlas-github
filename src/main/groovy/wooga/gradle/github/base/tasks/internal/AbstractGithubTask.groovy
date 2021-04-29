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
    private final Property<String> repositoryName

    @Override
    Property<String> getRepositoryName() {
        repositoryName
    }

    @Override
    void setRepositoryName(Provider<String> value) {
        repositoryName.set(value)
    }

    void setRepositoryName(String name) {
        if (!GithubRepositoryValidator.validateRepositoryName(name)) {
            throw new IllegalArgumentException("Repository value '$name' is not a valid github repository name. Expecting `owner/repo`.")
        }

        repositoryName.set(name)
    }
    
    @Optional
    @Input
    private final Property<String> baseUrl

    @Override
    Property<String> getBaseUrl() {
        baseUrl
    }

    @Override
    void setBaseUrl(Provider<String> value) {
        baseUrl.set(value)
    }
    
    @Optional
    @Input
    private final Property<String> username

    @Override
    Property<String> getUsername() {
        username
    }

    @Override
    void setUsername(Provider<String> value) {
        username.set(value)
    }

    @Optional
    @Input
    private final Property<String> password

    @Override
    Property<String> getPassword() {
        password
    }

    @Override
    void setPassword(Provider<String> value) {
        password.set(value)
    }

    @Optional
    @Input
    private final Property<String> token

    @Override
    Property<String> getToken() {
        token
    }

    @Override
    void setToken(Provider<String> value) {
        token.set(value)
    }

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


}
