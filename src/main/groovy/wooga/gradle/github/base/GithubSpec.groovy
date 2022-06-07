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

import com.wooga.gradle.BaseSpec
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import wooga.gradle.github.base.internal.GithubClientFactory
import wooga.gradle.github.base.internal.GithubRepositoryValidator
import wooga.gradle.github.base.tasks.Github

/**
 * Base Task spec definitions for a github tasks/actions.
 */
trait GithubSpec extends BaseSpec {

    private final Property<String> repositoryName = objects.property(String)

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
     * @see GithubBasePluginConvention#repositoryName
     */
    @Input
    Property<String> getRepositoryName() {
        repositoryName
    }

    /**
     * Sets the github repository name.
     * <p>
     * The given value must be a valid github repository name in the form of {@code owner/repositoryname}.
     *
     * @param name the repository name. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setRepositoryName(Provider<String> name) {
        repositoryName.set(name)
    }

    void setRepositoryName(String name) {
        if (!GithubRepositoryValidator.validateRepositoryName(name)) {
            throw new IllegalArgumentException("Repository value '$name' is not a valid github repository name. Expecting `owner/repo`.")
        }

        repositoryName.set(name)
    }

    private final Property<String> baseUrl = objects.property(String)

    /**
     * Returns the github api base url.
     *
     * @return the base url
     * @default https://api.github.com
     */
    @Optional
    @Input
    Property<String> getBaseUrl() {
        baseUrl
    }

    /**
     * Sets the github api base url.
     *
     * @param baseUrl the base url for github api. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setBaseUrl(Provider<String> baseUrl) {
        this.baseUrl.set(baseUrl)
    }

    final Property<String> username = objects.property(String)

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
     * @see GithubBasePluginConvention#userName
     */
    @Optional
    @Input
    Property<String> getUsername() {
        username
    }

    /**
     * Sets the github username.
     *
     * @param username the username. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setUsername(Provider<String> username) {
        this.username.set(username)
    }

    private final Property<String> password = objects.property(String)

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
     * @see GithubBasePluginConvention#password
     */
    @Optional
    @Input
    Property<String> getPassword() {
        password
    }

    /**
     * Sets the github user password.
     *
     * @param password the password. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setPassword(Provider<String> password) {
        this.password.set(password)
    }

    private final Property<String> token = objects.property(String)

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
     * @see GithubBasePluginConvention#token
     */
    @Optional
    @Input
    Property<String> getToken() {
        token
    }

    /**
     * Sets the github access token.
     *
     * @param token the token. Must not be {@code Null} or {@code empty}
     * @return this* @throws IllegalArgumentException
     */
    void setToken(Provider<String> token) {
        this.token.set(token)
    }

    private final Property<String> branchName = objects.property(String)

    /**
     * Returns the current git branch. Default value is in order of precedence:
     * <ul>
     *     <li>current branch's remote branch</li>
     *     <li>current branch</li>
     * </ul>
     *
     * <p>
     * The value can also be set via gradle properties.
     * The precedence order is:
     * <ul>
     *    <li><b>direct parameter in build.gradle code</b>
     *    <li><b>gradle properties</b>
     * </ul>
     * @return the current branch name. May be {@code Null} if there is no set up git repository
     * @see GithubBasePluginConvention#branchName
     */
    @Internal
    Provider<String> getBranchName() {
        branchName
    }

    private final Property<GitHub> clientProvider = objects.property(GitHub)

    /**
     * Gets a client for github REST API operations, using
     * credential providers set on this object in the following order:
     * 1. username and password,
     * 2. username and token
     * 3. token,
     * 4. external credentials (environment variables, .github file, etc)
     *
     * See org.kohsuke.github.GitHub for more details on the client.
     * @return Provider for github client with given credentials
     * @throws IOException if no credentials are found
     */
    @Internal
    Property<GitHub> getClientProvider() {
        clientProvider
    }
}
