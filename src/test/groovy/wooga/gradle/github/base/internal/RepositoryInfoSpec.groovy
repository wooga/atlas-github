package wooga.gradle.github.base.internal

import nebula.test.ProjectSpec
import org.ajoberstar.grgit.Branch
import org.ajoberstar.grgit.Grgit
import org.ajoberstar.grgit.Remote
import org.ajoberstar.grgit.service.BranchService
import org.ajoberstar.grgit.service.RemoteService
import spock.lang.Unroll

class RepositoryInfoSpec extends ProjectSpec {

    @Unroll("gets repository name based on local git 'origin' remote url #remoteURL")
    def "gets repository name based on local git 'origin' remote"() {
        given: "a gradle project"
        and: "a local git with set 'origin' remote"
        def git = Grgit.init(dir: project.projectDir)
        git.remote.add(name: "origin", url: remoteURL, pushUrl: remoteURL)


        when: "creating new repository info"
        def repoInfo = new RepositoryInfo(project, git)
        def repoName = repoInfo.repositoryNameFromLocalGit

        then:
        repoName.present
        repoName.get() == expectedRepositoryName

        where:
        remoteURL                              | expectedRepositoryName
        "https://github.com/user/repo.git"     | "user/repo"
        "https://www.github.com/user/repo.git" | "user/repo"
        "git@github.com:user/repo.git"         | "user/repo"
    }

    def "gets empty repository name provider if there is no local git 'origin' remote"(){
        given: "a gradle project"
        and: "a local git without set 'origin' remote"
        def git = Grgit.init(dir: project.projectDir)

        when: "getting repository name form repository info"
        def repoInfo = new RepositoryInfo(project, git)
        def repoName = repoInfo.repositoryNameFromLocalGit

        then:
        !repoName.present
    }

    def "gets empty providers if there is no local git repository"(){
        given: "a gradle project"
        and: "without local git"

        when: "getting repository name form repository info"
        def repoInfo = new RepositoryInfo(project, null as Grgit)

        then:
        !repoInfo.repositoryNameFromLocalGit.present
        !repoInfo.branchNameFromLocalGit.present
    }


    def "gets empty repository name if current git remote is not from github.com"(){
        given: "a gradle project"
        and: "a local git with set 'origin' remote not from github.com"
        def remoteURL = "https://bitbucket.org/repo"
        def git = Grgit.init(dir: project.projectDir)
        git.remote.add(name: "origin", url: remoteURL, pushUrl: remoteURL)


        when: "getting repository name form repository info"
        def repoInfo = new RepositoryInfo(project, git)

        then:
        !repoInfo.repositoryNameFromLocalGit.present
        repoInfo.branchNameFromLocalGit.present
    }

    @Unroll
    def "creates repository info with expected branch"() {
        given: "git installation"
        def git = mockedGrgitOnBranch(localBranch, trackingBranch)

        when:
        def infoFactory = new RepositoryInfo(project, git)
        def infoProvider = infoFactory.branchNameFromLocalGit

        then:
        infoProvider.present
        infoProvider.get() == expBranch

        where:
        localBranch | trackingBranch | expBranch
        "branch"    | null           | "branch"
        "branch"    | "tracking"     | "tracking"
    }

    def mockedGrgitOnBranch(String currentBranchName, String currentTrackingBranchName,
                            String remoteURL = "github.com:cmp/repo") {
        def gitMock = GroovyMock(Grgit)

        def branchService = GroovyMock(BranchService)
        def branchMock = GroovyMock(Branch)
        gitMock.branch >> branchService
        branchService.current() >> branchMock
        branchMock.name >> currentBranchName
        if (currentTrackingBranchName) {
            def trackingBranch = GroovyMock(Branch)
            trackingBranch.name >> currentTrackingBranchName
            branchMock.trackingBranch >> trackingBranch
        }

        def remoteService = GroovyMock(RemoteService)
        def remoteMock = GroovyMock(Remote)
        gitMock.remote >> remoteService
        remoteMock.url >> remoteURL
        remoteService.list() >> [remoteMock]

        return gitMock
    }

}
