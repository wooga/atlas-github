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


interface GithubSpec {

    String getUserName()

    GithubSpec setUserName(String userName)

    GithubSpec userName(String userName)

    String getPassword()

    GithubSpec setPassword(String password)

    GithubSpec password(String password)

    String getToken()

    GithubSpec setToken(String token)

    GithubSpec Token(String token)

    String getRepository()

    GithubSpec setRepository(String repo)

    GithubSpec repository(String repo)

    String getBaseUrl()

    GithubSpec setBaseUrl(String baseUrl)

    GithubSpec baseUrl(String baseUrl)
}