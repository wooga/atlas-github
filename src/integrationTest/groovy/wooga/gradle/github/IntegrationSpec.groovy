package wooga.gradle.github

import nebula.test.functional.ExecutionResult

class IntegrationSpec extends nebula.test.IntegrationSpec {

    def setup() {
        def gradleVersion = System.getenv("GRADLE_VERSION")
        if (gradleVersion) {
            this.gradleVersion = gradleVersion
            fork = true
        }
    }

    Boolean outputContains(ExecutionResult result, String message) {
        result.standardOutput.contains(message) || result.standardError.contains(message)
    }
}
