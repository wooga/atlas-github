package wooga.gradle.github.base

import org.gradle.api.GradleException
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.kohsuke.github.GHRepository
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder

abstract class AbstractGithubTask<T extends AbstractGithubTask> extends ConventionTask implements GithubSpec {

    private String repository
    private String baseUrl
    private String userName
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

            if (getUserName() && getPassword()) {
                builder = builder.withPassword(getUserName(), getPassword())
            } else if (getUserName() && getToken()) {
                builder = builder.withOAuthToken(getToken(), getUserName())

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
            repository = client.getRepository(getRepository())
        }
        catch (Exception e) {
            throw new GradleException("can't find repository ${getRepository()}")
        }
        repository
    }

    @Override
    String getRepository() {
        return repository
    }

    @Override
    T setRepository(String repository) {
        if (repository == null || repository.isEmpty()) {
            throw new IllegalArgumentException("repository")
        }

        if (!GithubRepositoryValidator.validateRepositoryName(repository)) {
            throw new IllegalArgumentException("Repository value '$repository' is not a valid github repository name. Expecting `owner/repo`.")
        }

        this.repository = repository
        return taskType.cast(this)
    }

    @Override
    T repository(String repo) {
        return taskType.cast(this.setRepository(repo))
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
    String getUserName() {
        return this.userName
    }

    @Override
    T setUserName(String userName) {
        this.userName = userName
        return taskType.cast(this)
    }

    @Override
    T userName(String userName) {
        this.setUserName(userName)
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
