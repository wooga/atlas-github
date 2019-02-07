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

import org.apache.tika.detect.Detector
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import org.apache.tika.parser.AutoDetectParser
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.kohsuke.github.GHAsset
import org.kohsuke.github.GHRelease
import org.kohsuke.github.HttpException

class ReleaseAssetUpload {

    private static final Logger logger = Logging.getLogger(ReleaseAssetUpload.class)

    static GHAsset uploadAsset(GHRelease release, File assetFile) {
        def contentType = getAssetContentType(assetFile)
        FileInputStream s = new FileInputStream(assetFile)
        String fileName = URLEncoder.encode(assetFile.name, "UTF-8")

        uploadAssetRetry(release, fileName, s, contentType)
    }

    static GHAsset uploadAssetRetry(GHRelease release, String fileName, InputStream stream, String contentType, int retryCount = 0) {
        try {
            GHAsset asset = release.uploadAsset(fileName, stream, contentType)
            String uncodedFileName = URLDecoder.decode(fileName, "UTF-8")
            if (asset.name != uncodedFileName) {
                logger.warn("asset '${uncodedFileName}' renamed by github to '${asset.name}'")
            }

            return asset

        } catch(HttpException e) {
            if (e.responseCode == 502 && retryCount < 3) {
                uploadAssetRetry(release, fileName, stream, contentType, ++retryCount)
            }
            throw e
        }
    }

    protected static String getAssetContentType(File assetFile) {
        InputStream is = new FileInputStream(assetFile)
        BufferedInputStream bis = new BufferedInputStream(is)
        String contentType = "text/plain"
        try {
            AutoDetectParser parser = new AutoDetectParser()
            Detector detector = parser.getDetector()
            Metadata md = new Metadata()
            md.add(Metadata.RESOURCE_NAME_KEY, assetFile.name)
            MediaType mediaType = detector.detect(bis, md)
            contentType = mediaType.toString()
        }
        finally {

        }

        contentType
    }

}
