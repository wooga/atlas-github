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
    static final PropertyLookup userName = new PropertyLookup(null, "github.username", null)

    /**
     * Gradle property name to set the default value for {@code password}.
     * @value "github.password"
     * @see GithubSpec#getPassword()
     */
    static final PropertyLookup password = new PropertyLookup(null, "github.password", null)

    /**
     * Gradle property name to set the default value for {@code token}.
     * @value "github.token"
     * @see GithubSpec#getToken()
     */
    static final PropertyLookup token = new PropertyLookup(null, "github.token", null)

    /**
     * Gradle property name to set the default value for {@code repository}.
     * @value "github.repository"
     * @see GithubSpec#getRepositoryName()
     */
    static final PropertyLookup repositoryName = new PropertyLookup(null, "github.repositoryName", null)

    /**
     * Gradle property name to set the default value for {@code baseUrl}.
     * @value "github.baseUrl"
     * @see GithubSpec#getBaseUrl()
     */
    static final PropertyLookup baseUrl = new PropertyLookup(null, "github.baseUrl", null)

    /**
     * Gradle property name to set the default value for {@code branchName}.
     * @value "github.branchName"
     * @see GithubSpec#getBranchName()
     */
    static final PropertyLookup branchName = new PropertyLookup(null, "github.branch.name", null)

}
