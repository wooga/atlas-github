package wooga.gradle.github.base.internal


import org.ajoberstar.grgit.Grgit
import org.gradle.api.Project
import org.gradle.api.provider.Provider

class RepositoryInfo {

    private static final String GITHUB_DOMAIN = "github.com"
    private static final String DEFAULT_REMOTE = "origin"

    final Project project
    final Provider<Grgit> grgitProvider

    RepositoryInfo(Project project, Grgit git) {
        this(project, project.provider{git})
    }

    RepositoryInfo(Project project, Provider<Grgit> grgitProvider) {
        this.project = project
        this.grgitProvider = grgitProvider
    }

    Provider<String> getRepositoryNameFromLocalGit() {
        return grgitProvider.map { git ->
            def remote = git.remote.list().find {it.name == DEFAULT_REMOTE && it.url.contains(GITHUB_DOMAIN)}?: null
            if (remote == null) {
                return null
            }
            def remoteURL = remote.url
            def domainIndex = remoteURL.indexOf(GITHUB_DOMAIN)
            def urlAfterDomain = remoteURL.substring(domainIndex + GITHUB_DOMAIN.length() + 1)
            return urlAfterDomain.replace(".git", "")
        }
    }

    Provider<String> getBranchNameFromLocalGit() {
        return grgitProvider.map { git ->
            def currentBranch = git.branch.current()
            if (currentBranch == null) {
                return null
            }
            def branch = currentBranch.trackingBranch != null ? currentBranch.trackingBranch : currentBranch
            if (branch == null) {
                return null
            }
            branch.name
        }
    }
}
