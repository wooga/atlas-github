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
    def "retrieve default values from properties when set #valueToTest:#propertyValue"() {
        given: "project properties with values"
        Map<String, String> properties = [:]
        if (propertyValue) {
            properties[propertyKey] = propertyValue
        }
        and: "a project mocked with properties"
        project.properties >> properties
        project.hasProperty(propertyKey) >> (propertyValue != null)

        expect:
        extension.getProperty(valueToTest) == propertyValue

        where:
        valueToTest  | propertyKey                                              | propertyValue
        "userName"   | DefaultGithubPluginExtention.GITHUB_USER_NAME_OPTION     | null
        "userName"   | DefaultGithubPluginExtention.GITHUB_USER_NAME_OPTION     | "testUser"
        "password"   | DefaultGithubPluginExtention.GITHUB_USER_PASSWORD_OPTION | null
        "password"   | DefaultGithubPluginExtention.GITHUB_USER_PASSWORD_OPTION | "testPassword"
        "token"      | DefaultGithubPluginExtention.GITHUB_TOKEN_OPTION         | null
        "token"      | DefaultGithubPluginExtention.GITHUB_TOKEN_OPTION         | "testToken"
        "repository" | DefaultGithubPluginExtention.GITHUB_REPOSITORY_OPTION    | "test/repo"
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
    def "setter throws IllegalArgumentException"() {
        when:

        if (useSetter) {
            extension.setProperty(valueToTest, propertyValue)
        } else {
            extension.invokeMethod(valueToTest, propertyValue)
        }

        then:
        IllegalArgumentException e = thrown()
        e.message == valueToTest

        where:
        valueToTest  | propertyValue | useSetter
        "repository" | null          | true
        "repository" | null          | false
        "repository" | ""            | true
        "repository" | ""            | false
        "token"      | null          | true
        "token"      | null          | false
        "token"      | ""            | true
        "token"      | ""            | false

        propertyMessage = propertyValue == null ? propertyValue : "empty"
        methodName = useSetter ? "set" + valueToTest.capitalize() : valueToTest
    }
}
