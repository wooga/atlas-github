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
        return repositoryName
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
        return taskType.cast(this)
    }

    @Override
    T repositoryName(String name) {
        return taskType.cast(this.setRepositoryName(name))
    }

    @Optional
    @Input
    @Override
    String getBaseUrl() {
        return baseUrl
    }

    @Override
    T setBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl")
        }
        this.baseUrl = baseUrl
        return taskType.cast(this)
    }

    @Override
    T baseUrl(String baseUrl) {
        return taskType.cast(this.setBaseUrl(baseUrl))
    }

    @Optional
    @Input
    @Override
    String getToken() {
        return this.token
    }

    @Override
    T setToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("token")
        }
        this.token = token
        return taskType.cast(this)
    }

    @Override
    T token(String token) {
        return taskType.cast(this.setToken(token))
    }

    @Optional
    @Input
    @Override
    String getUsername() {
        return this.username
    }

    @Override
    T setUsername(String userName) {
        this.username = userName
        return taskType.cast(this)
    }

    @Override
    T username(String username) {
        this.setUsername(username)
        return taskType.cast(this)
    }

    @Optional
    @Input
    @Override
    String getPassword() {
        return this.password
    }

    @Override
    T setPassword(String password) {
        this.password = password
        return taskType.cast(this)
    }

    @Override
    T password(String password) {
        this.setPassword(password)
        return taskType.cast(this)
    }
}
