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

import wooga.gradle.github.base.GithubPluginExtention
import wooga.gradle.github.base.GithubSpec

class DefaultGithubPluginExtension implements GithubPluginExtention {

    static final String GITHUB_USER_NAME_OPTION = "github.username"
    static final String GITHUB_USER_PASSWORD_OPTION = "github.password"
    static final String GITHUB_TOKEN_OPTION = "github.token"
    static final String GITHUB_REPOSITORY_OPTION = "github.repository"

    private String repositoryName
    private String baseUrl

    private String username
    private String password
    private String token

    private Map<String, ?> properties

    DefaultGithubPluginExtension(Map<String, ?> properties) {
        this.properties = properties
    }

    @Override
    String getUsername() {
        this.username ?: properties[GITHUB_USER_NAME_OPTION]
    }

    @Override
    DefaultGithubPluginExtension setUsername(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("username")
        }

        this.username = username
        this
    }

    @Override
    GithubSpec username(String username) {
        setUsername(username)
    }

    @Override
    String getPassword() {
        this.password ?: properties[GITHUB_USER_PASSWORD_OPTION]
    }

    @Override
    DefaultGithubPluginExtension setPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password")
        }

        this.password = password
        this
    }

    @Override
    GithubSpec password(String password) {
        setPassword(password)
    }

    @Override
    String getRepositoryName() {
        String value = this.repositoryName
        if (!this.repositoryName && properties[GITHUB_REPOSITORY_OPTION]) {
            value = properties[GITHUB_REPOSITORY_OPTION]
        }

        if (value && !GithubRepositoryValidator.validateRepositoryName(value)) {
            throw new IllegalArgumentException("Repository value '$value' is not a valid github repository name. Expecting `owner/repo`.")
        }

        value
    }

    @Override
    DefaultGithubPluginExtension setRepositoryName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("repository")
        }

        if (!GithubRepositoryValidator.validateRepositoryName(name)) {
            throw new IllegalArgumentException("Repository value '$name' is not a valid github repository name. Expecting `owner/repo`.")
        }

        this.repositoryName = name
        this
    }

    @Override
    DefaultGithubPluginExtension repositoryName(String name) {
        setRepositoryName(name)
    }

    @Override
    String getBaseUrl() {
        baseUrl
    }

    @Override
    DefaultGithubPluginExtension setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl")
        }

        this.baseUrl = baseUrl
        this
    }

    @Override
    DefaultGithubPluginExtension baseUrl(String baseUrl) {
        setBaseUrl(baseUrl)
    }

    @Override
    String getToken() {
        this.token ?: properties[GITHUB_TOKEN_OPTION]
    }

    @Override
    DefaultGithubPluginExtension setToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("token")
        }
        this.token = token
        this
    }

    @Override
    DefaultGithubPluginExtension token(String token) {
        setToken(token)
    }
}
