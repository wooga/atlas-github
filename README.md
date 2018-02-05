atlas-github
============

[![Gradle Plugin ID](https://img.shields.io/badge/gradle-net.wooga.github-brightgreen.svg?style=flat-square)](https://plugins.gradle.org/plugin/net.wooga.github)
[![Build Status](https://img.shields.io/travis/wooga/atlas-github/master.svg?style=flat-square)](https://travis-ci.org/wooga/atlas-github)
[![Coveralls Status](https://img.shields.io/coveralls/wooga/atlas-github/master.svg?style=flat-square)](https://coveralls.io/github/wooga/atlas-github?branch=master)
[![Apache 2.0](https://img.shields.io/badge/license-Apache%202-blue.svg?style=flat-square)](https://raw.githubusercontent.com/wooga/atlas-github/master/LICENSE)
[![GitHub tag](https://img.shields.io/github/tag/wooga/atlas-github.svg?style=flat-square)]()
[![GitHub release](https://img.shields.io/github/release/wooga/atlas-github.svg?style=flat-square)]()

This plugin provides tasks and conventions to publish artifacts to github with the help of [github-api.kohsuke.org][github-api].

# Applying the plugin

**build.gradle**
```groovy
plugins {
    id 'net.wooga.github' version '0.1.0'
}
```

Usage
=====

**build.gradle**

```groovy
plugins {
    id "net.wooga.github" version "0.1.0"
}

github {
    userName = "wooga"
    password = "a password."
    token "a github access token"
    repositoryName "wooga/atlas-github"
    baseUrl = null
}

githubPublish {
    userName = "wooga"
    password = "a password."
    token "a github access token"
    repositoryName "wooga/atlas-github"
    baseUrl = null
    targetCommitish = "master"
    tagName = project.version
    releaseName = project.version
    body = null
    prerelease = false
    draft = false

    //copySpec values
    from() {
        into
    }
}
```

Documentation
=============

- [API docs](https://wooga.github.io/atlas-github/docs/api/)
- [Tasks](docs/Tasks.md)


Gradle and Java Compatibility
=============================

Built with Oracle JDK7
Tested with Oracle JDK8

| Gradle Version | Works       |
| :------------- | :---------: |
| <= 2.13        | ![no]       |
| 2.14           | ![yes]      |
| 3.0            | ![yes]      |
| 3.1            | ![yes]      |
| 3.2            | ![yes]      |
| 3.4            | ![yes]      |
| 3.4.1          | ![yes]      |
| 3.5            | ![yes]      |
| 3.5.1          | ![yes]      |
| 4.0            | ![yes]      |

Development
===========

#### Running the tests
The integration tests will access github with a botUser which credentials needs to be provided via environment variables.

| Name                                | Description                                                                          |
| ----------------------------------- | ------------------------------------------------------------------------------------ |
| `ATLAS_GITHUB_INTEGRATION_USER`     | The username for of the BotUser. The name will also be used for the test repository. |
| `ATLAS_GITHUB_INTEGRATION_PASSWORD` | A password for the gihub user or acces token.                                        |

If the value of `ATLAS_GITHUB_INTEGRATION_PASSWORD` is a access token, it needs the scopes: `delete_repo`, `repo`
see [github-oauth-scopes] for more information.

LICENSE
=======

Copyright 2017 Wooga GmbH

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

<!-- Links -->
[github]:               https://github.com
[github-env-auth]:      http://github-api.kohsuke.org/apidocs/org/kohsuke/github/GitHubBuilder.html#fromEnvironment--
[github-cred-auth]:     http://github-api.kohsuke.org/apidocs/org/kohsuke/github/GitHubBuilder.html#fromCredentials--
[yes]:                  http://atlas-resources.wooga.com/icons/icon_check.svg "yes"
[no]:                   http://atlas-resources.wooga.com/icons/icon_uncheck.svg "no"
[github-api]:           http://github-api.kohsuke.org/source-repository.html
[copy-spec]:            https://docs.gradle.org/3.4/javadoc/org/gradle/api/file/CopySpec.html
[copy-spec-into]:       https://docs.gradle.org/3.4/javadoc/org/gradle/api/file/CopySpec.html#into(java.lang.Object)
[copy-destinationDir]:  https://docs.gradle.org/current/dsl/org.gradle.api.tasks.Copy.html#org.gradle.api.tasks.Copy:destinationDir
[github-oauth-scopes]:  https://developer.github.com/apps/building-integrations/setting-up-and-registering-oauth-apps/about-scopes-for-oauth-apps/
