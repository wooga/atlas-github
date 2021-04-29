/*
 * Copyright 2018-2021 Wooga GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

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
