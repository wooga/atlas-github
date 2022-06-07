package wooga.gradle.github

import com.wooga.gradle.test.PropertyLocation
import com.wooga.gradle.test.queries.TestValue
import com.wooga.gradle.test.writers.PropertyGetterTaskWriter
import com.wooga.gradle.test.writers.PropertySetInvocation
import com.wooga.gradle.test.writers.PropertySetterWriter
import org.ajoberstar.grgit.Grgit
import spock.lang.Shared
import spock.lang.Unroll
import wooga.gradle.github.publish.GithubPublishPluginConvention

import static wooga.gradle.github.base.GithubBasePluginConvention.*
import static wooga.gradle.github.base.GithubBasePluginConvention.userName

class GithubPluginExtensionIntegrationSpec extends IntegrationSpec {

    @Shared
    String currentBranch
    @Shared
    String remoteRepo
    @Shared
    Grgit git

    def setupSpec() {
        remoteRepo = "owner/repo"
        currentBranch = "branch"
    }

    def cleanupSpec() {
        git.close()
    }

    def setup() {
        buildFile << """
        ${applyPlugin(GithubPlugin)}
        """.stripIndent()
        git = Grgit.init(dir: projectDir)
        def remoteURL = "https://github.com/${remoteRepo}.git"
        git.remote.add(name: "origin", url: remoteURL, pushUrl: remoteURL)
        git.commit(message: "initial")
        git.checkout(branch: currentBranch, createBranch: true)
    }

    @Unroll()
    def "extension property #property returns '#value'"() {

        expect:
        runPropertyQuery(getter, setter).matches(value)

        where:
        property         | method                            | value                                     | type               | location
        "repositoryName" | PropertySetInvocation.method      | "testUser/testRepo"                       | "String"           | PropertyLocation.property
        "repositoryName" | PropertySetInvocation.assignment  | "testUser/testRepo"                       | "String"           | PropertyLocation.script
        "repositoryName" | PropertySetInvocation.method      | "testUser/testRepo"                       | "Provider<String>" | PropertyLocation.script
        "repositoryName" | PropertySetInvocation.providerSet | "testUser/testRepo"                       | "String"           | PropertyLocation.script
        "repositoryName" | PropertySetInvocation.providerSet | "testUser/testRepo"                       | "Provider<String>" | PropertyLocation.script
        "repositoryName" | PropertySetInvocation.setter      | "testUser/testRepo"                       | "String"           | PropertyLocation.script
        "repositoryName" | PropertySetInvocation.setter      | "testUser/testRepo"                       | "Provider<String>" | PropertyLocation.script
        "repositoryName" | PropertySetInvocation.method      | TestValue.set(null).expect(remoteRepo)    | "String"           | PropertyLocation.none

        "username"       | PropertySetInvocation.method      | "someUser2"                               | "String"           | PropertyLocation.property
        "username"       | PropertySetInvocation.assignment  | "someUser3"                               | "String"           | PropertyLocation.script
        "username"       | PropertySetInvocation.method      | "someUser4"                               | "Provider<String>" | PropertyLocation.script
        "username"       | PropertySetInvocation.providerSet | "someUser5"                               | "String"           | PropertyLocation.script
        "username"       | PropertySetInvocation.providerSet | "someUser6"                               | "Provider<String>" | PropertyLocation.script
        "username"       | PropertySetInvocation.setter      | "someUser7"                               | "String"           | PropertyLocation.script
        "username"       | PropertySetInvocation.setter      | "someUser8"                               | "Provider<String>" | PropertyLocation.script
        "username"       | PropertySetInvocation.method      | null                                      | "String"           | PropertyLocation.none

        "password"       | PropertySetInvocation.method      | "userPass2"                               | "String"           | PropertyLocation.property
        "password"       | PropertySetInvocation.assignment  | "userPass3"                               | "String"           | PropertyLocation.script
        "password"       | PropertySetInvocation.method      | "userPass4"                               | "Provider<String>" | PropertyLocation.script
        "password"       | PropertySetInvocation.providerSet | "userPass5"                               | "String"           | PropertyLocation.script
        "password"       | PropertySetInvocation.providerSet | "userPass6"                               | "Provider<String>" | PropertyLocation.script
        "password"       | PropertySetInvocation.setter      | "userPass7"                               | "String"           | PropertyLocation.script
        "password"       | PropertySetInvocation.setter      | "userPass8"                               | "Provider<String>" | PropertyLocation.script
        "password"       | PropertySetInvocation.method      | null                                      | "String"           | PropertyLocation.none

        "token"          | PropertySetInvocation.method      | "token2"                                  | "String"           | PropertyLocation.property
        "token"          | PropertySetInvocation.assignment  | "token3"                                  | "String"           | PropertyLocation.script
        "token"          | PropertySetInvocation.method      | "token4"                                  | "Provider<String>" | PropertyLocation.script
        "token"          | PropertySetInvocation.providerSet | "token5"                                  | "String"           | PropertyLocation.script
        "token"          | PropertySetInvocation.providerSet | "token6"                                  | "Provider<String>" | PropertyLocation.script
        "token"          | PropertySetInvocation.setter      | "token7"                                  | "String"           | PropertyLocation.script
        "token"          | PropertySetInvocation.setter      | "token8"                                  | "Provider<String>" | PropertyLocation.script
        "token"          | PropertySetInvocation.method      | null                                      | "String"           | PropertyLocation.none

        "baseUrl"        | PropertySetInvocation.method      | "https://api.github.com/2"                | "String"           | PropertyLocation.property
        "baseUrl"        | PropertySetInvocation.assignment  | "https://api.github.com/3"                | "String"           | PropertyLocation.script
        "baseUrl"        | PropertySetInvocation.method      | "https://api.github.com/4"                | "Provider<String>" | PropertyLocation.script
        "baseUrl"        | PropertySetInvocation.providerSet | "https://api.github.com/5"                | "String"           | PropertyLocation.script
        "baseUrl"        | PropertySetInvocation.providerSet | "https://api.github.com/6"                | "Provider<String>" | PropertyLocation.script
        "baseUrl"        | PropertySetInvocation.setter      | "https://api.github.com/7"                | "String"           | PropertyLocation.script
        "baseUrl"        | PropertySetInvocation.setter      | "https://api.github.com/8"                | "Provider<String>" | PropertyLocation.script
        "baseUrl"        | PropertySetInvocation.method      | null                                      | "String"           | PropertyLocation.none

        "branchName"     | PropertySetInvocation.none        | TestValue.set(null).expect(currentBranch) | "String"           | PropertyLocation.none

        extensionName = "github"
        setter = new PropertySetterWriter(extensionName, property)
            .set(value, type)
            .to(location)
            .use(method)

        getter = new PropertyGetterTaskWriter(setter)
    }

    @Unroll
    def "extension property #extProperty should return value #value when set as gradle property #gradleProperty"() {

        expect:
        runPropertyQuery(getter, setter).matches(value)

        where:
        extProperty      | gradleProperty          | value
        "repositoryName" | "github.repositoryName" | "owner/reponame"
        "username"       | "github.username"       | "user"
        "password"       | "github.password"       | "pword"
        "token"          | "github.token"          | "tkn"
        "baseUrl"        | "github.baseUrl"        | "url"
        "branchName"     | "github.branch.name"    | "branch"

        extensionName = "github"
        setter = new PropertySetterWriter(extensionName, extProperty)
            .set(value, String)
            .to(PropertyLocation.property)
        getter = new PropertyGetterTaskWriter(setter)
    }

}
