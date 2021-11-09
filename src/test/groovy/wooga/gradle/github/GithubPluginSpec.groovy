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

package wooga.gradle.github

import nebula.test.ProjectSpec
import org.gradle.api.publish.plugins.PublishingPlugin
import spock.lang.Unroll
import wooga.gradle.github.base.GithubBasePlugin
import wooga.gradle.github.base.GithubPluginExtension
import wooga.gradle.github.publish.tasks.GithubPublish
import wooga.gradle.github.publish.GithubPublishPlugin

class GithubPluginSpec extends ProjectSpec {

    public static final String PLUGIN_NAME = 'net.wooga.github'

    @Unroll("creates the task #extensionName extension")
    def 'Creates the extensions'() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.extensions.findByName(extensionName)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def extension = project.extensions.findByName(extensionName)
        extensionType.isInstance(extension)

        where:
        extensionName                   | extensionType
        GithubBasePlugin.EXTENSION_NAME | GithubPluginExtension.class
    }

    @Unroll("creates the task #taskName")
    def 'Creates needed tasks'(String taskName, Class taskType) {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.tasks.findByName(taskName)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def task = project.tasks.findByName(taskName)
        taskType.isInstance(task)

        where:
        taskName                              | taskType
        GithubPublishPlugin.PUBLISH_TASK_NAME | GithubPublish

    }

    @Unroll("applies plugin #pluginName")
    def 'Applies other plugins'(String pluginName, Class pluginType) {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)
        assert !project.plugins.hasPlugin(pluginType)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        project.plugins.hasPlugin(pluginType)

        where:
        pluginName   | pluginType
        "githubBase" | GithubBasePlugin
        "publish"    | PublishingPlugin
    }

    def "Sets publish lifecycle"() {
        given:
        assert !project.plugins.hasPlugin(PLUGIN_NAME)

        when:
        project.plugins.apply(PLUGIN_NAME)

        then:
        def githubPublishTask = project.tasks.named(GithubPublishPlugin.PUBLISH_TASK_NAME)
        def publishTasks = project.tasks.findByName(PublishingPlugin.PUBLISH_LIFECYCLE_TASK_NAME)
        publishTasks.dependsOn.contains(githubPublishTask)
    }
}
