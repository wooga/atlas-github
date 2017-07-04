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

import org.gradle.api.Project

class DefaultGithubPluginExtention implements GithubPluginExtention {
    static final String GITHUB_USER_NAME_OPTION = "github.userName"
    static final String GITHUB_USER_PASSWORD_OPTION = "github.password"
    static final String GITHUB_TOKEN_OPTION = "github.token"
    static final String GITHUB_REPOSITORY_OPTION = "github.repository"

    private String repository
    private String baseUrl

    private String userName
    private String password
    private String token

    private Map<String, ?> properties

    DefaultGithubPluginExtention(Map<String, ?> properties) {
        this.properties = properties
    }

    @Override
    String getUserName() {
        if (!this.repository && properties[GITHUB_USER_NAME_OPTION]) {
            return properties[GITHUB_USER_NAME_OPTION]
        }

        return this.userName
    }

    @Override
    GithubSpec setUserName(String userName) {
        if (userName == null || userName.isEmpty()) {
            throw new IllegalArgumentException("userName")
        }

        this.userName = userName
        return this
    }

    @Override
    GithubSpec userName(String userName) {
        return setUserName(userName)
    }

    @Override
    String getPassword() {
        if (!this.repository && properties[GITHUB_USER_PASSWORD_OPTION]) {
            return properties[GITHUB_USER_PASSWORD_OPTION]
        }

        return this.password
    }

    @Override
    GithubSpec setPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("password")
        }

        this.password = password
        return this
    }

    @Override
    GithubSpec password(String password) {
        return setPassword(password)
    }

    @Override
    String getRepository() {
        String value = this.repository
        if (!this.repository && properties[GITHUB_REPOSITORY_OPTION]) {
            value = properties[GITHUB_REPOSITORY_OPTION]
        }

        if (!GithubRepositoryValidator.validateRepositoryName(value)) {
            throw new IllegalArgumentException("Repository value '$value' is not a valid github repository name. Expecting `owner/repo`.")
        }

        return value
    }

    @Override
    DefaultGithubPluginExtention setRepository(String repository) {
        if (repository == null || repository.isEmpty()) {
            throw new IllegalArgumentException("repository")
        }

        if (!GithubRepositoryValidator.validateRepositoryName(repository)) {
            throw new IllegalArgumentException("Repository value '$repository' is not a valid github repository name. Expecting `owner/repo`.")
        }

        this.repository = repository
        return this
    }

    @Override
    DefaultGithubPluginExtention repository(String repo) {
        return setRepository(repo)
    }

    @Override
    String getBaseUrl() {
        return baseUrl
    }

    @Override
    DefaultGithubPluginExtention setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl")
        }

        this.baseUrl = baseUrl
        return this
    }

    @Override
    DefaultGithubPluginExtention baseUrl(String baseUrl) {
        return setBaseUrl(baseUrl)
    }

    @Override
    String getToken() {
        if (!this.token && properties[GITHUB_TOKEN_OPTION]) {
            return properties[GITHUB_TOKEN_OPTION]
        }
        return this.token
    }

    @Override
    DefaultGithubPluginExtention setToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("token")
        }
        this.token = token
        return this
    }

    @Override
    DefaultGithubPluginExtention token(String token) {
        return setToken(token)
    }
}
