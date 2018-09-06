/*
 * Copyright 2018 Wooga GmbH
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

package wooga.gradle.github.publish

import org.kohsuke.github.GHRepository

/**
 * A strategy used to retrieve the github release description.
 * <p>
 * Example:
 * <pre>
 *     class ReleaseBody implements PublishBodyStrategy {
 *         @Override
 *         String getBody(GHRepository repository) {
 *             repository.listCommits().asList().collect { GHCommit commit ->
 *                 "[${commit.SHA1}] - ${commit.commitShortInfo.message}"
 *             }.join("\n")
 *         }
 *     }
 * }
 * </pre>
 */
interface PublishBodyStrategy {

    /**
     * Returns a release description text.
     * <p>
     * The given {@link GHRepository} object can be used to query Git commits etc.
     *
     * @param  repository the current github repository object
     * @return the release description
     */
    String getBody(GHRepository repository)
}