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

    String envNameFromProperty(String extensionName, String property) {
        "${extensionName.toUpperCase()}_${property.replaceAll(/([A-Z])/, "_\$1").toUpperCase()}"
    }

    @Unroll()
    def "extension property :#property returns '#testValue' if #reason"() {
        given:
        buildFile << """
            task(custom) {
                doLast {
                    def value = ${extensionName}.${property}.getOrNull()
                    println("${extensionName}.${property}: " + value)
                }
            }
        """

        and: "a gradle.properties"
        def propertiesFile = createFile("gradle.properties")

        switch (location) {
            case PropertyLocation.script:
                buildFile << "${extensionName}.${invocation}"
                break
            case PropertyLocation.property:
                propertiesFile << "${extensionName}.${property} = ${escapedValue}"
                break
            case PropertyLocation.env:
                environmentVariables.set(envNameFromProperty(extensionName, property), "${value}")
                break
            default:
                break
        }

        and: "the test value with replace placeholders"
        if (testValue instanceof String) {
            testValue = testValue.replaceAll("#projectDir#", escapedPath(projectDir.path))
        }

        when: ""
        def result = runTasksSuccessfully("custom")

        then:
        result.standardOutput.contains("${extensionName}.${property}: ${testValue}")

        where:
        property         | method               | rawValue                   | expectedValue | type               | location                  | additionalInfo
        "repositoryName" | _                    | "testUser/testRepo"        | _             | _                  | PropertyLocation.property | ""
        "repositoryName" | _                    | "testUser/testRepo"        | _             | "String"           | PropertyLocation.script   | ""
        "repositoryName" | _                    | "testUser/testRepo"        | _             | "Provider<String>" | PropertyLocation.script   | ""
        "repositoryName" | "repositoryName.set" | "testUser/testRepo"        | _             | "String"           | PropertyLocation.script   | ""
        "repositoryName" | "repositoryName.set" | "testUser/testRepo"        | _             | "Provider<String>" | PropertyLocation.script   | ""
        "repositoryName" | "setRepositoryName"  | "testUser/testRepo"        | _             | "String"           | PropertyLocation.script   | ""
        "repositoryName" | "setRepositoryName"  | "testUser/testRepo"        | _             | "Provider<String>" | PropertyLocation.script   | ""
        "repositoryName" | _                    | _                          | null          | _                  | PropertyLocation.none     | ""

        "username"       | _                    | "someUser2"                | _             | _                  | PropertyLocation.property | ""
        "username"       | _                    | "someUser3"                | _             | "String"           | PropertyLocation.script   | ""
        "username"       | _                    | "someUser4"                | _             | "Provider<String>" | PropertyLocation.script   | ""
        "username"       | "username.set"       | "someUser5"                | _             | "String"           | PropertyLocation.script   | ""
        "username"       | "username.set"       | "someUser6"                | _             | "Provider<String>" | PropertyLocation.script   | ""
        "username"       | "setUsername"        | "someUser7"                | _             | "String"           | PropertyLocation.script   | ""
        "username"       | "setUsername"        | "someUser8"                | _             | "Provider<String>" | PropertyLocation.script   | ""
        "username"       | _                    | _                          | null          | _                  | PropertyLocation.none     | ""

        "password"       | _                    | "userPass2"                | _             | _                  | PropertyLocation.property | ""
        "password"       | _                    | "userPass3"                | _             | "String"           | PropertyLocation.script   | ""
        "password"       | _                    | "userPass4"                | _             | "Provider<String>" | PropertyLocation.script   | ""
        "password"       | "password.set"       | "userPass5"                | _             | "String"           | PropertyLocation.script   | ""
        "password"       | "password.set"       | "userPass6"                | _             | "Provider<String>" | PropertyLocation.script   | ""
        "password"       | "setPassword"        | "userPass7"                | _             | "String"           | PropertyLocation.script   | ""
        "password"       | "setPassword"        | "userPass8"                | _             | "Provider<String>" | PropertyLocation.script   | ""
        "password"       | _                    | _                          | null          | _                  | PropertyLocation.none     | ""

        "token"          | _                    | "token2"                   | _             | _                  | PropertyLocation.property | ""
        "token"          | _                    | "token3"                   | _             | "String"           | PropertyLocation.script   | ""
        "token"          | _                    | "token4"                   | _             | "Provider<String>" | PropertyLocation.script   | ""
        "token"          | "token.set"          | "token5"                   | _             | "String"           | PropertyLocation.script   | ""
        "token"          | "token.set"          | "token6"                   | _             | "Provider<String>" | PropertyLocation.script   | ""
        "token"          | "setToken"           | "token7"                   | _             | "String"           | PropertyLocation.script   | ""
        "token"          | "setToken"           | "token8"                   | _             | "Provider<String>" | PropertyLocation.script   | ""
        "token"          | _                    | _                          | null          | _                  | PropertyLocation.none     | ""

        "baseUrl"        | _                    | "https://api.github.com/2" | _             | _                  | PropertyLocation.property | ""
        "baseUrl"        | _                    | "https://api.github.com/3" | _             | "String"           | PropertyLocation.script   | ""
        "baseUrl"        | _                    | "https://api.github.com/4" | _             | "Provider<String>" | PropertyLocation.script   | ""
        "baseUrl"        | "baseUrl.set"        | "https://api.github.com/5" | _             | "String"           | PropertyLocation.script   | ""
        "baseUrl"        | "baseUrl.set"        | "https://api.github.com/6" | _             | "Provider<String>" | PropertyLocation.script   | ""
        "baseUrl"        | "setBaseUrl"         | "https://api.github.com/7" | _             | "String"           | PropertyLocation.script   | ""
        "baseUrl"        | "setBaseUrl"         | "https://api.github.com/8" | _             | "Provider<String>" | PropertyLocation.script   | ""
        "baseUrl"        | _                    | _                          | null          | _                  | PropertyLocation.none     | ""

        extensionName = "github"
        value = (type != _) ? wrapValueBasedOnType(rawValue, type.toString()) : rawValue
        providedValue = (location == PropertyLocation.script) ? type : value
        testValue = (expectedValue == _) ? rawValue : expectedValue
        reason = location.reason() + ((location == PropertyLocation.none) ? "" : "  with '$providedValue' ") + additionalInfo
        escapedValue = (value instanceof String) ? escapedPath(value) : value
        invocation = (method != _) ? "${method}(${escapedValue})" : "${property} = ${escapedValue}"
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
        if (!result.success) {
            outputContains(result, expectedError)
        }

        where:
        repositoryName | expectedError
        'some value'   | "Repository value 'some value' is not a valid github repository name. Expecting `owner/repo`"
    }
}
