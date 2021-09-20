package wooga.gradle.github.base.internal


import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class RepositoryInfo {

    private static final String DOMAIN = "github.com"
    private static final String DEFAULT_REMOTE = "origin"

    final Project project
    final Grgit git

    RepositoryInfo(Project project, Grgit git) {
        this.project = project
        this.git = git
    }

    Provider<String> repositoryNameProviderFromLocalGit() {
        return project.provider {
            git.remote.list().find {it.name == DEFAULT_REMOTE}?: null
        }.map{remote ->
            def remoteURL = remote.url
            def domainIndex = remoteURL.indexOf(DOMAIN)
            def urlAfterDomain = remoteURL.substring(domainIndex + DOMAIN.length() + 1)
            return urlAfterDomain.replace(".git", "")
        }
    }

    Provider<String> branchNameProviderFromLocalGit() {
        def currentBranch = git.branch.current()
        return project.provider { currentBranch.trackingBranch }.
                        orElse(currentBranch).
                        map {it.name }
    }
}
