/*
 * Copyright 2019 Wooga GmbH
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

package wooga.gradle.github.publish.internal


import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.kohsuke.github.GHRelease

class GithubPublishRollbackHandler {
    private static final Logger logger = Logging.getLogger(GithubPublishRollbackHandler)

    static void rollback(GHRelease release, boolean deleteRelease, Throwable cause = null) {
        logger.error("publish github release failed")
        logger.error("attempt rollback")
        if (deleteRelease && release) {
            logger.info("delete created release")
            try {
                release.delete()
            } catch (Exception error) {
                logger.error("failed to rollback release")
                logger.error(error.message)
            }
        }

        if(release && cause && cause.cause && GithubReleaseUploadAssetException.isInstance(cause.cause) && !deleteRelease) {
            GithubReleaseUploadAssetException rootCause = cause.cause as GithubReleaseUploadAssetException
            rootCause.uploadedAssets.each {
                try {
                    logger.info("delete published asset ${it.name}")
                    it.delete()
                } catch (Exception error) {
                    logger.error("failed to rollback asset ${it.name}")
                    logger.error(error.message)
                }
            }

            rootCause.updatedAssets.each {
                try {
                    logger.info("restore updated asset ${it.name}")
                    it.restore(release)
                } catch (Exception error) {
                    logger.error("failed to restore asset ${it.name}")
                    logger.error(error.message)
                }
            }
        }
    }
}
