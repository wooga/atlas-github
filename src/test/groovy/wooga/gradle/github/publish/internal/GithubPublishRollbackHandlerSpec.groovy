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
import spock.lang.Specification
import spock.lang.Unroll
import wooga.gradle.github.publish.tasks.GithubPublish

class GithubPublishRollbackHandlerSpec extends Specification {


    @Unroll
    def "rollback tries to deletes release and catches all errors"() {
        given: "a delete counter"
        def count = 0
        and: "a release that might fail on delete"

        release.delete() >> {
            count++; if (deleteWillFail) {
                throw new IOException("some error")
            }
        }

        when:
        GithubPublishRollbackHandler.rollback(release, deleteRelease, null)

        then:
        if (deleteRelease && release) {
            count == 1
        } else {
            count == 0
        }

        noExceptionThrown()


        where:
        release         | deleteRelease | deleteWillFail
        Mock(GHRelease) | true          | true
        Mock(GHRelease) | true          | false
        Mock(GHRelease) | false         | true
        Mock(GHRelease) | false         | false
        null            | true          | false
        null            | false         | false
    }

    def "rollback deletes uploaded assets and throws no errors"() {
        given: "a delete counter"
        def count = 0

        and: "a list of uploaded Assets"
        List<GHAsset> assets = [Mock(GHAsset), Mock(GHAsset)]

        and: "a release Mock"
        def release = Mock(GHRelease)

        and: "a delete method mock"
        assets.each {
            it.delete() >> {
                count++; if (deleteWillFail) {
                    throw new IOException()
                }
            }
        }

        and: "a GithubReleaseUploadAssetException cause"
        def cause = new GithubReleaseUploadAssetException(assets, [], null)

        and: "a generic error with assets cause as cause"
        def error = new Exception("some exception", cause)

        when:
        GithubPublishRollbackHandler.rollback(release, false, error)

        then:
        noExceptionThrown()
        count == 2

        where:
        deleteWillFail << [true, false]
    }

    def "rollback restores updated assets and throws no errors"() {
        given: "a restore counter"
        def count = 0

        and: "a list of uploaded Assets"
        List<GithubPublish.UpdatedAsset> assets = [Mock(GithubPublish.UpdatedAsset), Mock(GithubPublish.UpdatedAsset)]

        and: "a release Mock"
        def release = Mock(GHRelease)

        and: "a delete method mock"
        assets.each {
            it.restore(release) >> {
                count++; if (restoreWillFail) {
                    throw new IOException()
                }
            }
        }

        and: "a GithubReleaseUploadAssetException cause"
        def cause = new GithubReleaseUploadAssetException([], assets, null)

        and: "a generic error with assets cause as cause"
        def error = new Exception("some exception", cause)

        when:
        GithubPublishRollbackHandler.rollback(release, false, error)

        then:
        noExceptionThrown()
        count == 2

        where:
        restoreWillFail << [true, false]
    }
}
