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
import spock.lang.Specification
import spock.lang.Unroll

class DefaultGithubPluginExtentionSpec extends Specification {

    def project = Mock(Project)
    def extension = new DefaultGithubPluginExtention(project)

    @Unroll
    def "retrieve default values from properties when set #userNameValue:#passwordValue:#tokenValue:#repositoryValue"() {
        given: "project properties with values"
        Map<String, String> properties = [:]
        if (userNameValue) {
            properties[DefaultGithubPluginExtention.GITHUB_USER_NAME_OPTION] = userNameValue
            project.hasProperty(DefaultGithubPluginExtention.GITHUB_USER_NAME_OPTION) >> true
        }

        if (passwordValue) {
            properties[DefaultGithubPluginExtention.GITHUB_USER_PASSWORD_OPTION] = passwordValue
            project.hasProperty(DefaultGithubPluginExtention.GITHUB_USER_PASSWORD_OPTION) >> true
        }

        if (tokenValue) {
            properties[DefaultGithubPluginExtention.GITHUB_TOKEN_OPTION] = tokenValue
            project.hasProperty(DefaultGithubPluginExtention.GITHUB_TOKEN_OPTION) >> true
        }

        if (repositoryValue) {
            properties[DefaultGithubPluginExtention.GITHUB_REPOSITORY_OPTION] = repositoryValue
            project.hasProperty(DefaultGithubPluginExtention.GITHUB_REPOSITORY_OPTION) >> true
        }

        and: "a project mocked with properties"
        project.properties >> properties


        expect:
        with(extension) {
            userName == userNameValue
            getUserName() == userNameValue

            password == passwordValue
            getPassword() == passwordValue

            token == tokenValue
            getToken() == tokenValue

            repository == repositoryValue
            getRepository() == repositoryValue
        }

        where:
        userNameValue | passwordValue  | tokenValue  | repositoryValue
        "testUser"    | "testPassword" | "testToken" | "test/repo"
        null          | null           | null        | "test/repo"
    }

    @Unroll
    def "retrieve default values from properties when set repository:#propertyValue"() {
        given: "project properties with values"
        Map<String, String> properties = [:]
        if (propertyValue) {
            properties[propertyKey] = propertyValue
        }
        and: "a project mocked with properties"
        project.properties >> properties
        project.hasProperty(propertyKey) >> (propertyValue != null)

        when:
        extension.repository

        then:
        IllegalArgumentException e = thrown()
        e.message.contains("Repository value '$propertyValue' is not a valid github repository name")

        where:
        valueToTest  | propertyKey                                           | propertyValue
        "repository" | DefaultGithubPluginExtention.GITHUB_REPOSITORY_OPTION | null
        "repository" | DefaultGithubPluginExtention.GITHUB_REPOSITORY_OPTION | "invalid-repo-name"
        "repository" | DefaultGithubPluginExtention.GITHUB_REPOSITORY_OPTION | "https://github.com/some/repo"
    }

    @Unroll("#methodName throws IllegalArgumentException when value is #propertyMessage")
    def "token throws IllegalArgumentException"() {
        when:
        if (useSetter) {
            extension.setToken(propertyValue)
        } else {
            extension.token(propertyValue)
        }

        then:
        IllegalArgumentException e = thrown()
        e.message == "token"

        where:
        propertyValue | useSetter
        null          | true
        null          | false
        ""            | true
        ""            | false

        propertyMessage = propertyValue == null ? propertyValue : "empty"
        methodName = useSetter ? "setToken" : "token"
    }

    @Unroll("#methodName throws IllegalArgumentException when value is #propertyMessage")
    def "repository setter throws IllegalArgumentException"() {
        when:
        if (useSetter) {
            extension.setRepository(propertyValue)
        } else {
            extension.repository(propertyValue)
        }

        then:
        IllegalArgumentException e = thrown()
        e.message == "repository"

        where:
        propertyValue | useSetter
        null          | true
        null          | false
        ""            | true
        ""            | false

        propertyMessage = propertyValue == null ? propertyValue : "empty"
        methodName = useSetter ? "setRepository" : "repository"
    }

    @Unroll("#methodName throws IllegalArgumentException when value is #propertyMessage")
    def "userName setter throws IllegalArgumentException"() {
        when:
        if (useSetter) {
            extension.setUserName(propertyValue)
        } else {
            extension.userName(propertyValue)
        }

        then:
        IllegalArgumentException e = thrown()
        e.message == "userName"

        where:
        propertyValue | useSetter
        null          | true
        null          | false
        ""            | true
        ""            | false

        propertyMessage = propertyValue == null ? propertyValue : "empty"
        methodName = useSetter ? "setUserName" : "userName"
    }

    @Unroll("#methodName throws IllegalArgumentException when value is #propertyMessage")
    def "password setter throws IllegalArgumentException"() {
        when:
        if (useSetter) {
            extension.setPassword(propertyValue)
        } else {
            extension.password(propertyValue)
        }

        then:
        IllegalArgumentException e = thrown()
        e.message == "password"

        where:
        propertyValue | useSetter
        null          | true
        null          | false
        ""            | true
        ""            | false

        propertyMessage = propertyValue == null ? propertyValue : "empty"
        methodName = useSetter ? "setPassword" : "password"
    }

    @Unroll("#methodName throws IllegalArgumentException when value is #propertyMessage")
    def "baseUrl setter throws IllegalArgumentException"() {
        when:
        if (useSetter) {
            extension.setBaseUrl(propertyValue)
        } else {
            extension.baseUrl(propertyValue)
        }

        then:
        IllegalArgumentException e = thrown()
        e.message == "baseUrl"

        where:
        propertyValue | useSetter
        null          | true
        null          | false
        ""            | true
        ""            | false

        propertyMessage = propertyValue == null ? propertyValue : "empty"
        methodName = useSetter ? "setBaseUrl" : "baseUrl"
    }

}
