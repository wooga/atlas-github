package wooga.gradle.github.tasks

import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import com.wooga.gradle.test.writers.PropertySetterWriter
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

        expect:
        runPropertyQuery(getter, setter)

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

        setter = new PropertySetterWriter(testTaskName, property)
            .set(rawValue, type)
            .toScript()

        getter = new PropertyGetterTaskWriter(setter)
    }

}
