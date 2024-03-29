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

import com.wooga.gradle.PropertyLookup

/**
 * Constant values for github plugin.
 */
class GithubBasePluginConvention {

    /**
     * Gradle property name to set the default value for {@code username}.
     * @value "github.username"
     * @see GithubSpec#getUsername()
     */
    static final PropertyLookup userName = new PropertyLookup(null, GITHUB_USER_NAME_OPTION, null)
    static final String GITHUB_USER_NAME_OPTION = "github.username"

    /**
     * Gradle property name to set the default value for {@code password}.
     * @value "github.password"
     * @see GithubSpec#getPassword()
     */
    static final PropertyLookup password = new PropertyLookup(null, GITHUB_USER_PASSWORD_OPTION, null)
    static final String GITHUB_USER_PASSWORD_OPTION = "github.password"

    /**
     * Gradle property name to set the default value for {@code token}.
     * @value "github.token"
     * @see GithubSpec#getToken()
     */
    static final PropertyLookup token = new PropertyLookup(null, GITHUB_TOKEN_OPTION, null)
    static final String GITHUB_TOKEN_OPTION = "github.token"

    /**
     * Gradle property name to set the default value for {@code repository}.
     * @value "github.repository"
     * @see GithubSpec#getRepositoryName()
     */
    static final PropertyLookup repositoryName = new PropertyLookup(null, GITHUB_REPOSITORY_NAME_OPTION, null)
    static final String GITHUB_REPOSITORY_NAME_OPTION = "github.repositoryName"

    /**
     * Gradle property name to set the default value for {@code baseUrl}.
     * @value "github.baseUrl"
     * @see GithubSpec#getBaseUrl()
     */
    static final PropertyLookup baseUrl = new PropertyLookup(null, GITHUB_BASE_URL_OPTION, null)
    static final String GITHUB_BASE_URL_OPTION = "github.baseUrl"

    /**
     * Gradle property name to set the default value for {@code branchName}.
     * @value "github.branchName"
     * @see GithubSpec#getBranchName()
     */
    static final PropertyLookup branchName = new PropertyLookup(null, GITHUB_BRANCH_NAME_OPTION, null)
    static final String GITHUB_BRANCH_NAME_OPTION = "github.branch.name"
}
