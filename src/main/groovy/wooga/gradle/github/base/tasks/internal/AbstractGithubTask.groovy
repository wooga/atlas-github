package wooga.gradle.github.base.tasks.internal

import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import wooga.gradle.github.base.internal.GithubRepositoryValidator
import wooga.gradle.github.base.GithubSpec

abstract class AbstractGithubTask<T extends AbstractGithubTask> extends ConventionTask implements GithubSpec {

    private String repositoryName
    private String baseUrl
    private String username
    private String password
    private String token

    private GitHub client

    private final Class<T> taskType

    AbstractGithubTask(Class<T> taskType) {
        this.taskType = taskType
    }

    protected GitHub getClient() {
        if(!this.client) {
            def builder = new GitHubBuilder()

            if (getUsername() && getPassword()) {
                builder = builder.withPassword(getUsername(), getPassword())
            } else if (getUsername() && getToken()) {
                builder = builder.withOAuthToken(getToken(), getUsername())

            } else if (getToken()) {
                builder = builder.withOAuthToken(getToken())

            } else {
                builder = GitHubBuilder.fromCredentials()
            }

            if (getBaseUrl()) {
                builder = builder.withEndpoint(getBaseUrl())
            }

            this.client = builder.build()
        }

        return client
    }

    GHRepository getRepository(GitHub client) {
        GHRepository repository
        try {
            repository = client.getRepository(getRepositoryName())
        }
        catch (Exception e) {
            throw new GradleException("can't find repository ${getRepositoryName()}")
        }
        repository
    }

    GHRepository getRepository() {
        GHRepository repository
        try {
            repository = getClient().getRepository(getRepositoryName())
        }
        catch (Exception e) {
            throw new GradleException("can't find repository ${getRepositoryName()}")
        }
        repository
    }

    @Override
    String getRepositoryName() {
        repositoryName
    }

    @Override
    T setRepositoryName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("repository")
        }

        if (!GithubRepositoryValidator.validateRepositoryName(name)) {
            throw new IllegalArgumentException("Repository value '$name' is not a valid github repository name. Expecting `owner/repo`.")
        }

        this.repositoryName = name
        taskType.cast(this)
    }

    @Override
    T repositoryName(String name) {
        taskType.cast(this.setRepositoryName(name))
    }

    @Optional
    @Input
    @Override
    String getBaseUrl() {
        baseUrl
    }

    @Override
    T setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl")
        }
        this.baseUrl = baseUrl
        taskType.cast(this)
    }

    @Override
    T baseUrl(String baseUrl) {
        taskType.cast(this.setBaseUrl(baseUrl))
    }

    @Optional
    @Input
    @Override
    String getToken() {
        this.token
    }

    @Override
    T setToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("token")
        }
        this.token = token
        taskType.cast(this)
    }

    @Override
    T token(String token) {
        taskType.cast(this.setToken(token))
    }

    @Optional
    @Input
    @Override
    String getUsername() {
        this.username
    }

    @Override
    T setUsername(String userName) {
        this.username = userName
        taskType.cast(this)
    }

    @Override
    T username(String username) {
        this.setUsername(username)
        taskType.cast(this)
    }

    @Optional
    @Input
    @Override
    String getPassword() {
        this.password
    }

    @Override
    T setPassword(String password) {
        this.password = password
        taskType.cast(this)
    }

    @Override
    T password(String password) {
        this.setPassword(password)
        taskType.cast(this)
    }
}
