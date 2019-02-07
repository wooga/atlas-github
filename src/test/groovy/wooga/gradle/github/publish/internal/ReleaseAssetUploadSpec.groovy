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


import org.kohsuke.github.GHAsset
import org.kohsuke.github.GHRelease
import org.kohsuke.github.HttpException
import spock.lang.Shared
import spock.lang.Specification
import spock.util.environment.RestoreSystemProperties

@RestoreSystemProperties
class ReleaseAssetUploadSpec extends Specification {

    @Shared
    GHRelease release

    @Shared
    GHAsset publishedAsset

    def setup() {
        release = Mock(GHRelease)
        publishedAsset = Mock(GHAsset)
        System.clearProperty("net.wooga.github.publish.assetUpload.retries")
    }

    def "retries up to default value of 3"() {
        given: "a mock asset to publish"
        def asset = File.createTempFile("ReleaseAssetUploadSpec","publishAsset")
        asset << """some text content"""

        and: "a retry counter"
        def counter = 0

        and: "a mocked exception"
        release.uploadAsset(_,_,_) >> { ++counter; throw new HttpException("", 502, "", "") }

        when:
        ReleaseAssetUpload.uploadAsset(release, asset)

        then:
        thrown(HttpException)
        counter == 4
    }

    def "retries up to value defined in system properties"() {
        given: "a mock asset to publish"
        def asset = File.createTempFile("ReleaseAssetUploadSpec","publishAsset")
        asset << """some text content"""

        and: "a retry count defined in system properties"
        def maxRetries = 20
        System.setProperty('net.wooga.github.publish.assetUpload.retries', maxRetries.toString())

        and: "a retry counter"
        def counter = 0

        and: "a mocked exception"
        release.uploadAsset(_,_,_) >> { ++counter; throw new HttpException("", 502, "", "") }

        when:
        ReleaseAssetUpload.uploadAsset(release, asset)

        then:
        thrown(HttpException)
        counter == maxRetries + 1
    }

    def "returns asset after success"() {
        given: "a mock asset to publish"
        def asset = File.createTempFile("ReleaseAssetUploadSpec","publishAsset")
        asset << """some text content"""

        and: "a retry counter"
        def counter = 0

        and: "a mocked exception"
        release.uploadAsset(_,_,_) >> { ++counter; throw new HttpException("", 502, "", "") } >> publishedAsset

        when:
        def result = ReleaseAssetUpload.uploadAsset(release, asset)

        then:
        noExceptionThrown()
        result == publishedAsset
        counter == 1
    }
}
