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

package wooga.gradle.github.base.internal

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.kohsuke.github.GitHub
import wooga.gradle.github.base.GithubPluginExtension

class DefaultGithubPluginExtension implements GithubPluginExtension {

    final Property<String> repositoryName
    @Override
    void setRepositoryName(Provider<String> name) {
        this.repositoryName.set(name)
    }

    final Property<String> baseUrl
    @Override
    void setBaseUrl(Provider<String> baseUrl) {
        this.baseUrl.set(baseUrl)
    }

    final Property<String> username
    @Override
    void setUsername(Provider<String> username) {
        this.username.set(username)
    }

    final Property<String> password
    @Override
    void setPassword(Provider<String> password) {
        this.password.set(password)
    }

    final Property<String> token
    @Override
    void setToken(Provider<String> token) {
        this.token.set(token)
    }
    @Override @Internal
    Provider<GitHub> getClientProvider() {
        return GithubClientFactory.clientProvider(username, password, token).
        orElse(project.provider {
          throw new IOException("could not find valid credentials for github client")
        })
    }

    private final Project project

    DefaultGithubPluginExtension(Project project) {
        this.project = project

        repositoryName = project.objects.property(String)
        baseUrl = project.objects.property(String)
        username = project.objects.property(String)
        password = project.objects.property(String)
        token = project.objects.property(String)
    }
}
