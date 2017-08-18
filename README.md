atlas-github
===========

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

#Usage

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

## Tasks
| Task name          | Depends on            | Type                                           | Description |
| ------------------ | --------------------- | ---------------------------------------------- | ----------- |
| githubPublish      |                       | `wooga.gradle.github.publish.GithubPublish`    | Copies files and folder configured to temp directory and uploads them to github release |

The plugin will only add one task `githubPublish` which needs further configuration before it executes.

### Authentication
To authenticate with [github] you need to set either the `userName` and `token` parameter in the `github` plugin extension or in the `GithubPublish` task configuration.
If you specify the values in the extension configuration all tasks of type `wooga.gradle.github.publish.GithubPublish` will be configured with these values by default. You can create multiple publish tasks with different credentials or repository values.
You can also use [github-api.kohsuke.org][github-api] authentication fallback logic [fromCredentials][github-cred-auth] or [fromEnvironment][github-env-auth].

**kohsuke api docs fromEnvironment**
```
    The following environment variables are recognized:

    GITHUB_LOGIN: username like 'kohsuke'
    GITHUB_PASSWORD: raw password
    GITHUB_OAUTH: OAuth token to login
    GITHUB_ENDPOINT: URL of the API endpoint 
```

### Files to publish
The publishing process works in three steps.

1. copy files configured in [`CopySpec`][copy-spec] to temp directory. You can use all of the [`copy`][copy-spec] tasks methods, except [`into`][copy-spec-into] and [`destinationDir`][copy-destinationDir].
2. iterate items in `temp` directory and zip all directories (archive name is `${directory.name}.zip`) and copy files to second `temp` dir
3. create github release and upload all files (regular and compressed) in `temp` directory

#### examples

**build.gradle (publish single file)**
```groovy
githubPublish {
    description "publish a single file"
    from "testFiles/file1.txt"
    tagName = "singleFile"
}
```
In this example the plugin will upload only a single file referenced.

**build.gradle (publish all files in directory)**
```groovy
githubPublish {
    description "publish all files in a directory"
    from "testFiles/dirWithFiles"
    tagName = "allFilesInDirectory"
}
```
Gradle will iterate through `dirWithFiles` directory and upload all files located in this directory.

**build.gradle (publish directory as zip)**
```groovy
githubPublish {
    description "publish a directory as zip"
        from("testFiles/dirWithFiles") {
          into "package"
        }
        tagName = "directoryAsZip"
}
```
Here gradle will publish one zip `package.zip` to github.

**build.gradle (publish files and directories)**
```groovy
githubPublish {
    description "publish a directory as zip"
        description "publish all files in folder. Folders inside will get zipped before upload"
        from("testFiles")
        tagName = "wholeStructure"
}
```

**build.gradle (publish files in configuration)**
```groovy
dependencies {
    archive files("testFiles/files1/file1")
    archive files("testFiles/file1.txt")
}

githubPublish {
    description "publish a configuration"
    from(configurations.archive)
    tagName = "configuration"
}
```

## Generic Github Task

The type `wooga.gradle.github.base.Github` can be used to build generic tasks who can access the github API through [github-api.kohsuke.org][github-api]. It implements the `wooga.gradle.github.base.GithubSpec` interface and handles the basic github client creation and authentication.
The task type is usable for scripted tasks or can be extended. You should then use `wooga.gradle.github.base.AbstractGithubTask` instead.
Along with the properties from `wooga.gradle.github.base.GithubSpec` the task gives access to [`client`](http://github-api.kohsuke.org/apidocs/org/kohsuke/github/GitHub.html) and, if `repositoryName` is set, to [`repository`](http://github-api.kohsuke.org/apidocs/org/kohsuke/github/GHRepository.html) property.

**create repos**
```
task customGithubTask(type:wooga.gradle.github.base.Github) {
    doLast {
        def builder = client.createRepository("Repo")
        builder.description("description")
        builder.autoInit(false)
        builder.licenseTemplate('MIT')
        builder.private_(false)
        builder.issues(false)
        builder.wiki(false)
        builder.create()
    }
}
```

**update files**
```
task customGithubTask(type:wooga.gradle.github.base.Github) {
    doLast {
        def content = repository.getFileContent("$file")
        content.update("$updatedContent", "update release notes")
    }
}
```


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
