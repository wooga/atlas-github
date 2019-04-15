package wooga.gradle.github

import spock.lang.Unroll

class GithubPluginExtensionIntegrationSpec extends IntegrationSpec {

    def setup() {
        buildFile << """
        ${applyPlugin(GithubPlugin)}
        """.stripIndent()
    }

    enum PropertyLocation {
        none, script, property, env

        String reason() {
            switch (this) {
                case script:
                    return "value is provided in script"
                case property:
                    return "value is provided in props"
                case env:
                    return "value is set in env"
                default:
                    return "no value was configured"
            }
        }
    }

    enum PropertySetType {
        setter, setMethod, propertySet

        String reason() {
            switch (this) {
                case setter:
                    return "value is provided with setter"
                case setMethod:
                    return "value is with set method"
                case propertySet:
                    return "value is set in property with set"
            }
        }
    }

    String envNameFromProperty(String property) {
        "GITHUB_${property.replaceAll(/([A-Z])/, "_\$1").toUpperCase()}"
    }

    @Unroll(":#property returns '#testValue' if #reason")
    def "extension property :#property returns '#testValue' if #reason"() {
        given:
        buildFile << """
            task(custom) {
                doLast {
                    def value = "not set"
                    if(github.${property}.present) {
                      value = github.${property}.get()
                    }

                    println("github.${property}: " + value)
                }
            }
        """

        and: "a gradle.properties"
        def propertiesFile = createFile("gradle.properties")

        def escapedValue = (value instanceof String) ? escapedPath(value) : value

        switch (location) {
            case PropertyLocation.script:
                buildFile << "github.${property} = ${escapedValue}"
                break
            case PropertyLocation.property:
                propertiesFile << "github.${property} = ${escapedValue}"
                break
            case PropertyLocation.env:
                environmentVariables.set(envNameFromProperty(property), "${value}")
                break
            default:
                break
        }

        when: ""
        def result = runTasks("custom")

        then:
        result.success
        result.standardOutput.contains("github.${property}: ${testValue}")

        where:
        property         | value                      | expectedValue            | providedValue            | location
        "repositoryName" | "testUser/testRepo"        | _                        | "testUser/testRepo"      | PropertyLocation.property
        "repositoryName" | "'testUser/testRepo'"      | 'testUser/testRepo'      | "testUser/testRepo"      | PropertyLocation.script
        "repositoryName" | null                       | "not set"                | null                     | PropertyLocation.none

        "username"       | "testUser"                 | _                        | "testUser"               | PropertyLocation.property
        "username"       | "'testUser'"               | 'testUser'               | "testUser"               | PropertyLocation.script
        "username"       | null                       | "not set"                | null                     | PropertyLocation.none

        "password"       | "testPass"                 | _                        | "testPass"               | PropertyLocation.property
        "password"       | "'testPass'"               | 'testPass'               | "testPass"               | PropertyLocation.script
        "password"       | null                       | "not set"                | null                     | PropertyLocation.none

        "token"          | "accessToken"              | _                        | "accessToken"            | PropertyLocation.property
        "token"          | "'accessToken'"            | 'accessToken'            | "accessToken"            | PropertyLocation.script
        "token"          | null                       | "not set"                | null                     | PropertyLocation.none

        "baseUrl"        | "https://api.github.com"   | 'not set'                | "https://api.github.com" | PropertyLocation.property
        "baseUrl"        | "'https://api.github.com'" | 'https://api.github.com' | "https://api.github.com" | PropertyLocation.script
        "baseUrl"        | null                       | "not set"                | null                     | PropertyLocation.none

        testValue = (expectedValue == _) ? value : expectedValue
        reason = location.reason() + ((location == PropertyLocation.none) ? "" : " with '$providedValue'")
    }

    @Unroll
    def "can set #property with #type"() {
        given:
        buildFile << """
            task(custom) {
                doLast {
                    def value = "not set"
                    if(github.${property}.present) {
                      value = github.${property}.get()
                    }

                    println("github.${property}: " + value)
                }
            }
        """

        def escapedValue = (value instanceof String) ? escapedPath(value) : value

        switch (type) {
            case PropertySetType.setter:
                buildFile << "github.${property} = ${escapedValue}"
                break
            case PropertySetType.setMethod:
                buildFile << "github.${property} ${escapedValue}"
                break
            case PropertySetType.propertySet:
                buildFile << "github.${property}.set(${escapedValue})"
                break
        }

        when: ""
        def result = runTasks("custom")

        then:
        result.success
        result.standardOutput.contains("github.${property}: ${testValue}")

        where:
        property         | value                      | expectedValue            | type
        "repositoryName" | "'testUser/testRepo'"      | "testUser/testRepo"      | PropertySetType.setter
        "repositoryName" | "'testUser/testRepo'"      | "testUser/testRepo"      | PropertySetType.setMethod
        "repositoryName" | "'testUser/testRepo'"      | "testUser/testRepo"      | PropertySetType.propertySet

        "username"       | "'testUser'"               | "testUser"               | PropertySetType.setter
        "username"       | "'testUser'"               | "testUser"               | PropertySetType.setMethod
        "username"       | "'testUser'"               | "testUser"               | PropertySetType.propertySet

        "password"       | "'testPass'"               | "testPass"               | PropertySetType.setter
        "password"       | "'testPass'"               | "testPass"               | PropertySetType.setMethod
        "password"       | "'testPass'"               | "testPass"               | PropertySetType.propertySet

        "token"          | "'accessToken'"            | "accessToken"            | PropertySetType.setter
        "token"          | "'accessToken'"            | "accessToken"            | PropertySetType.setMethod
        "token"          | "'accessToken'"            | "accessToken"            | PropertySetType.propertySet

        "baseUrl"        | "'https://api.github.com'" | "https://api.github.com" | PropertySetType.setter
        "baseUrl"        | "'https://api.github.com'" | "https://api.github.com" | PropertySetType.setMethod
        "baseUrl"        | "'https://api.github.com'" | "https://api.github.com" | PropertySetType.propertySet

        testValue = (expectedValue == _) ? value : expectedValue
    }

    def "validates repoName property before set"() {
        given: ""
        buildFile << """
        github.repositoryName = "${repositoryName}"
        """.stripIndent()

        when:
        def result = runTasks("tasks")

        then:
        result.success == (expectedError == null)
        if(!result.success) {
            outputContains(result, expectedError)
        }

        where:
        repositoryName      | expectedError
        'some value'        | "Repository value 'some value' is not a valid github repository name. Expecting `owner/repo`"
    }
}
