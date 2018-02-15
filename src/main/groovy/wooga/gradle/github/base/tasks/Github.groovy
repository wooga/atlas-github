package wooga.gradle.github.base.tasks

import wooga.gradle.github.base.tasks.internal.AbstractGithubTask

/**
 * Execute arbitrary github API calls.
 * <p>
 * This type can be used to build generic tasks who can access the github API through github-api.kohsuke.org.
 * It implements the {@link wooga.gradle.github.base.GithubSpec} and handles the basic github client creation and
 * authentication. This type is usable for scripted tasks or can be extended.
 * Along with the properties from {@link wooga.gradle.github.base.GithubSpec} the task gives access to client and,
 * if repositoryName is set, to repository property.
 * <p>
 * Example:
 * <pre>
 * {@code
 *     task customGithub(type:wooga.gradle.github.base.tasks.Github) {
 *         doLast {
 *             def builder = client.createRepository("Repo")
 *             builder.description("description")
 *             builder.autoInit(false)
 *             builder.licenseTemplate('MIT')
 *             builder.private_(false)
 *             builder.issues(false)
 *             builder.wiki(false)
 *             builder.create()
 *         }
 *     }
 * }
 * </pre>
 */
class Github extends AbstractGithubTask {

    Github() {
        super(Github.class)
    }
}
