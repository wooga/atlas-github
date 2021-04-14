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

import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

/**
 * Base Task spec definitions for a github tasks/actions.
 */
interface GithubSpec {

    /**
     * Returns the github repository name.
     * <p>
     * The format must be in {@code owner/repositoryname}.
     *
     * The value can also be set via gradle properties.
     * The precedence order is:
     * <ul>
     *    <li><b>direct parameter in code</b>
     *    <li><b>gradle properties</b>
     * </ul>
     * @return the github repository name. May be {@code Null}
     * @see GithubBasePluginConvention#GITHUB_REPOSITORY_NAME_OPTION
     */
    Property<String> getRepositoryName()

    /**
     * Sets the github repository name.
     * <p>
     * The given value must be a valid github repository name in the form of {@code owner/repositoryname}.
     *
     * @param name the repository name. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setRepositoryName(Provider<String> name)

    /**
     * Returns the github api base url.
     *
     * @return the base url
     * @default https://api.github.com
     */
    Property<String> getBaseUrl()

    /**
     * Sets the github api base url.
     *
     * @param baseUrl the base url for github api. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setBaseUrl(Provider<String> baseUrl)

    /**
     * Returns the github username.
     * <p>
     * The value can also be set via gradle properties.
     * The precedence order is:
     * <ul>
     *    <li><b>direct parameter in code</b>
     *    <li><b>gradle properties</b>
     * </ul>
     * @return the github username. May be {@code Null}
     * @see GithubBasePluginConvention#GITHUB_USER_NAME_OPTION
     */
    Property<String> getUsername()

    /**
     * Sets the github username.
     *
     * @param username the username. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setUsername(Provider<String> username)

    /**
     * Returns the github user password.
     * <p>
     * The value can also be set via gradle properties.
     * The precedence order is:
     * <ul>
     *    <li><b>direct parameter in code</b>
     *    <li><b>gradle properties</b>
     * </ul>
     * @return the github username. May be {@code Null}
     * @see GithubBasePluginConvention#GITHUB_USER_PASSWORD_OPTION
     */
    Property<String> getPassword()

    /**
     * Sets the github user password.
     *
     * @param password the password. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setPassword(Provider<String> password)

    /**
     * Returns the github authentication token.
     * <p>
     * The value can also be set via gradle properties.
     * The precedence order is:
     * <ul>
     *    <li><b>direct parameter in code</b>
     *    <li><b>gradle properties</b>
     * </ul>
     * @return the github access token. May be {@code Null}
     * @see GithubBasePluginConvention#GITHUB_TOKEN_OPTION
     */
    Property<String> getToken()

    /**
     * Sets the github access token.
     *
     * @param token the token. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setToken(Provider<String> token)
}
