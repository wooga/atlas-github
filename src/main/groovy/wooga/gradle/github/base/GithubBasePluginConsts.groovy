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

package wooga.gradle.github.base

/**
 * Constant values for github plugin.
 */
class GithubBasePluginConsts {
    /**
     * Gradle property name to set the default value for {@code username}.
     * @value "github.username"
     * @see GithubSpec#username
     */
    static final String GITHUB_USER_NAME_OPTION = "github.username"

    /**
     * Gradle property name to set the default value for {@code password}.
     * @value "github.password"
     * @see GithubSpec#password
     */
    static final String GITHUB_USER_PASSWORD_OPTION = "github.password"

    /**
     * Gradle property name to set the default value for {@code token}.
     * @value "github.token"
     * @see GithubSpec#token
     */
    static final String GITHUB_TOKEN_OPTION = "github.token"

    /**
     * Gradle property name to set the default value for {@code repository}.
     * @value "github.repository"
     * @see GithubSpec#repositoryName
     */
    static final String GITHUB_REPOSITORY_OPTION = "github.repository"
}
