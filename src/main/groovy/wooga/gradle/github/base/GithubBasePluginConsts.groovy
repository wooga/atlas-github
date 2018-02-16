package wooga.gradle.github.base

/**
 * Constant values for github plugin.
 */
class GithubBasePluginConsts {
    /**
     * Gradle property name to set the default value for {@code username}.
     * @value "github.username"
     * @see GithubSpec#username
     */
    static final String GITHUB_USER_NAME_OPTION = "github.username"

    /**
     * Gradle property name to set the default value for {@code password}.
     * @value "github.password"
     * @see GithubSpec#password
     */
    static final String GITHUB_USER_PASSWORD_OPTION = "github.password"

    /**
     * Gradle property name to set the default value for {@code token}.
     * @value "github.token"
     * @see GithubSpec#token
     */
    static final String GITHUB_TOKEN_OPTION = "github.token"

    /**
     * Gradle property name to set the default value for {@code repository}.
     * @value "github.repository"
     * @see GithubSpec#repositoryName
     */
    static final String GITHUB_REPOSITORY_OPTION = "github.repository"
}
