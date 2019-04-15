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

package wooga.gradle.github.base.internal

import org.gradle.api.Project
import org.gradle.api.provider.Property
import wooga.gradle.github.base.GithubPluginExtention
import wooga.gradle.github.base.GithubSpec

class DefaultGithubPluginExtension implements GithubPluginExtention {

    final Property<String> repositoryName
    final Property<String> baseUrl

    final Property<String> username
    final Property<String> password
    final Property<String> token

    private final Closure property

    DefaultGithubPluginExtension(Project project) {
        this.property = project.&findProperty

        repositoryName = project.objects.property(String)
        baseUrl = project.objects.property(String)
        username = project.objects.property(String)
        password = project.objects.property(String)
        token = project.objects.property(String)
    }

    @Override
    DefaultGithubPluginExtension setUsername(String username) {
        this.username.set(username)
        this
    }

    @Override
    GithubSpec username(String username) {
        setUsername(username)
    }

    @Override
    DefaultGithubPluginExtension setPassword(String password) {
        this.password.set(password)
        this
    }

    @Override
    GithubSpec password(String password) {
        setPassword(password)
    }

    @Override
    DefaultGithubPluginExtension setRepositoryName(String name) {
        if (!GithubRepositoryValidator.validateRepositoryName(name)) {
            throw new IllegalArgumentException("Repository value '$name' is not a valid github repository name. Expecting `owner/repo`.")
        }

        this.repositoryName.set(name)
        this
    }

    @Override
    DefaultGithubPluginExtension repositoryName(String name) {
        setRepositoryName(name)
    }

    @Override
    DefaultGithubPluginExtension setBaseUrl(String baseUrl) {
        this.baseUrl.set(baseUrl)
        this
    }

    @Override
    DefaultGithubPluginExtension baseUrl(String baseUrl) {
        setBaseUrl(baseUrl)
    }

    @Override
    DefaultGithubPluginExtension setToken(String token) {
        this.token.set(token)
        this
    }

    @Override
    DefaultGithubPluginExtension token(String token) {
        setToken(token)
    }
}
