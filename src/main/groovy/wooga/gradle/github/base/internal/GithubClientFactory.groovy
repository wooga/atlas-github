package wooga.gradle.github.base.internal


import org.gradle.api.provider.Provider
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import wooga.gradle.github.base.GithubPluginExtension

class GithubClientFactory {

    static Provider<GitHub> clientProvider(Provider<String> username,
                                           Provider<String> password,
                                           Provider<String> token) {
        //a non-empty provider if any credentials are available
        Provider hasCredsProvider = username.orElse(password).orElse(token).orElse("").
        map({
            return it == "" && !hasExternalCredentials()? null : true
        })
        return hasCredsProvider.map({
            return new GithubClientFactory().
                    createGithubClient(username.getOrNull(), password.getOrNull(), token.getOrNull())
        })
    }

    private static boolean hasExternalCredentials() {
        try {
            GitHubBuilder.fromCredentials()
            return true
        } catch(IOException _) {
            return false
        }
    }

    GitHub createGithubClient(String username, String password, String token) {
        def builder = new GitHubBuilder()

        if (username && password) {
            builder = builder.withPassword(username, password)
        } else if (username && token) {
            builder = builder.withOAuthToken(token, username)

        } else if (token) {
            builder = builder.withOAuthToken(token)

        } else {
            builder = GitHubBuilder.fromCredentials()
        }
        return builder.build()
    }
}
