package wooga.gradle.github

class IntegrationSpec extends nebula.test.IntegrationSpec {

    def setup() {
        def gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion) {
            this.gradleVersion = gradleVersion
            fork = true
        }
    }
}
