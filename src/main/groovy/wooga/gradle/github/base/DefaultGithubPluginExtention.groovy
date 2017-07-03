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
    static final String GITHUB_OWNER_OPTION = "github.owner"
    static final String GITHUB_TOKEN_OPTION = "github.token"
    static final String GITHUB_REPOSITORY_OPTION = "github.repository"

    static final String GITHUB_USER_ENV_VAR = "GITHUB_USR"
    static final String GITHUB_PASSWORD_ENV_VAR = "GITHUB_PWD"
    static final String GITHUB_REPOSITORY_ENV_VAR = "GITHUB_REPOSITORY"

    private String repository
    private String baseUrl

    private String userName
    private String password
    private String token

    private Project project

    DefaultGithubPluginExtention(Project project) {
        this.project = project
    }

    @Override
    String getUserName() {
        return userName
    }

    @Override
    GithubSpec setUserName(String userName) {
        this.userName = userName
        return this
    }

    @Override
    GithubSpec userName(String userName) {
        this.setUserName(userName)
        return this
    }

    @Override
    String getPassword() {
        return this.password
    }

    @Override
    GithubSpec setPassword(String password) {
        this.password = password
        return this
    }

    @Override
    GithubSpec password(String password) {
        this.setPassword(password)
        return this
    }

    @Override
    String getRepository() {
        if (!this.repository && project.properties.hasProperty(GITHUB_REPOSITORY_OPTION)) {
            return project.properties[GITHUB_REPOSITORY_OPTION]
        }

        if (!GithubRepositoryValidator.validateRepositoryName(repository)) {
            throw new IllegalArgumentException("Repository value '$repository' is not a valid github repository name. Expecting `owner/repo`.")
        }

        return this.repository
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
        return this.setRepository(repo)
    }

    @Override
    String getBaseUrl() {
        return baseUrl
    }

    @Override
    DefaultGithubPluginExtention setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl
        return this
    }

    @Override
    DefaultGithubPluginExtention baseUrl(String baseUrl) {
        return this.setBaseUrl(baseUrl)
    }

    @Override
    String getToken() {
        if (!this.token && project.properties.hasProperty(GITHUB_TOKEN_OPTION)) {
            return project.properties[GITHUB_TOKEN_OPTION]
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
    DefaultGithubPluginExtention Token(String token) {
        return this.setToken(token)
    }
}
