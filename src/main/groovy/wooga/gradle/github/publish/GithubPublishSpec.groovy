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

package wooga.gradle.github.publish

import wooga.gradle.github.base.GithubSpec

interface GithubPublishSpec extends GithubSpec {

    String getTagName()

    GithubPublishSpec setTagName(String tagTame)

    GithubPublishSpec tagName(String tagTame)

    String getTargetCommitish()

    GithubPublishSpec setTargetCommitish(String targetCommitish)

    GithubPublishSpec targetCommitish(String targetCommitish)

    String getReleaseName()

    GithubPublishSpec setReleaseName(String name)

    GithubPublishSpec releaseName(String name)

    String getBody()

    GithubPublishSpec setBody(String body)

    GithubPublishSpec body(String body)

    boolean isPrerelease()

    GithubPublishSpec setPrerelease(boolean prerelease)

    GithubPublishSpec prerelease(boolean prerelease)

    boolean isDraft()

    GithubPublishSpec setDraft(boolean draft)

    GithubPublishSpec draft(boolean draft)
}