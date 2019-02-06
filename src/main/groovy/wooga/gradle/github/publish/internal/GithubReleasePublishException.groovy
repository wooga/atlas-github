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

import groovy.transform.InheritConstructors
import org.kohsuke.github.GHAsset
import org.kohsuke.github.GHRelease
import wooga.gradle.github.publish.tasks.GithubPublish

class GithubReleaseException extends Exception {
    final private GHRelease release

    GithubReleaseException(String message) {
        this(null, message)
    }

    GithubReleaseException(String message, Throwable cause) {
        this(null, message, cause)

    }

    GithubReleaseException(GHRelease release, String message) {
        this(release, message, null)
    }

    GithubReleaseException(GHRelease release, String message, Throwable cause) {
        super(message, cause)
        this.release = release
    }

    GHRelease getRelease() {
        this.release
    }
}

@InheritConstructors
class GithubReleaseCreateException extends GithubReleaseException {
}

@InheritConstructors
class GithubReleaseUpdateException extends GithubReleaseException {
}

@InheritConstructors
class GithubReleaseUploadAssetsException extends GithubReleaseException {

    GithubReleaseUploadAssetsException(GHRelease release, String message) {
        super(release, message)

    }

    GithubReleaseUploadAssetsException(GHRelease release, String message, Throwable cause) {
        super(release, message, cause)
    }
}

@InheritConstructors
class GithubReleaseUploadAssetException extends GithubReleaseException {

    List<GHAsset> uploadedAssets
    List<GithubPublish.UpdatedAsset> updatedAssets

    GithubReleaseUploadAssetException(List<GHAsset> uploadedAssets, List<GithubPublish.UpdatedAsset> updatedAssets, Throwable cause) {
        super("failed to restore asset", cause)

        this.uploadedAssets = uploadedAssets
        this.updatedAssets = updatedAssets
    }

}
