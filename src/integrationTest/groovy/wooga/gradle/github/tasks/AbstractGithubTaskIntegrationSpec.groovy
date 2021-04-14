package wooga.gradle.github.tasks

import spock.lang.Specification
import spock.lang.Unroll
import wooga.gradle.github.AbstractGithubIntegrationSpec

abstract class AbstractGithubTaskIntegrationSpec extends AbstractGithubIntegrationSpec {

    abstract String getTestTaskName()
    abstract Class getTestTaskType()

    def setup() {
        buildFile << """
            task ${testTaskName}(type:${testTaskType.name})                        
        """.stripIndent()
    }

    @Unroll("can set property #property with #method and type #type")
    def "can set base property"() {

        given: "a task to read back the value"
        buildFile << """
            task("readValue") {
                doLast {
                    println("property: " + ${testTaskName}.${property}.get())
                }
            }
        """.stripIndent()

        and: "a set property"
        buildFile << """
            ${testTaskName}.${method}($value)
        """.stripIndent()

        when:
        def result = runTasksSuccessfully("readValue")

        then:
        outputContains(result, "property: " + expectedValue.toString())

        where:
        property         | method               | rawValue | type
        "repositoryName" | "repositoryName.set" | "foo"    | "String"
        "repositoryName" | "repositoryName.set" | "bar"    | "Provider<String>"
        "repositoryName" | "setRepositoryName " | "bar"    | "Provider<String>"
        "baseUrl"        | "baseUrl.set"        | "foo"    | "String"
        "baseUrl"        | "baseUrl.set"        | "bar"    | "Provider<String>"
        "baseUrl"        | "setBaseUrl "        | "bar"    | "Provider<String>"
        "username"       | "username.set"       | "foo"    | "String"
        "username"       | "username.set"       | "bar"    | "Provider<String>"
        "username"       | "setUsername"        | "bar"    | "Provider<String>"
        "password"       | "password.set"       | "foo"    | "String"
        "password"       | "password.set"       | "bar"    | "Provider<String>"
        "password"       | "setPassword"        | "bar"    | "Provider<String>"
        "token"          | "token.set"          | "foo"    | "String"
        "token"          | "token.set"          | "bar"    | "Provider<String>"
        "token"          | "setToken"           | "bar"    | "Provider<String>"

        value = wrapValueBasedOnType(rawValue, type)
        expectedValue = rawValue
    }

}
