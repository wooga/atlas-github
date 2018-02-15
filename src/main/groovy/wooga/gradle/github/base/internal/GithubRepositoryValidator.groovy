/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package wooga.gradle.github.base.internal

import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class GithubRepositoryValidator {
    private static final Logger logger = Logging.getLogger(GithubRepositoryValidator)

    static Boolean validateRepositoryName(String repository) {
        logger.debug("validating repository value $repository")
        if (!repository) {
            logger.warn("can't validate null value")
            return false
        }

        def parts = repository.split('/')
        if (parts.size() != 2) {
            logger.warn("Repository value '$repository' is not a valid github repository name. Expecting `owner/repo`")
            return false
        }

        true
    }
}
