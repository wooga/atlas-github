# Tasks

| Task name          | Depends on            | Type                                           | Description |
| ------------------ | --------------------- | ---------------------------------------------- | ----------- |
| githubPublish      |                       | `wooga.gradle.github.publish.GithubPublish`    | Copies files and folder configured to temp directory and uploads them to github release |

The plugin will only add one task `githubPublish` which needs further configuration before it executes.

## Authentication
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

## Files to publish
The publishing process works in three steps.

1. copy files configured in [`CopySpec`][copy-spec] to temp directory. You can use all of the [`copy`][copy-spec] tasks methods, except [`into`][copy-spec-into] and [`destinationDir`][copy-destinationDir].
2. iterate items in `temp` directory and zip all directories (archive name is `${directory.name}.zip`) and copy files to second `temp` dir
3. create github release and upload all files (regular and compressed) in `temp` directory

### examples

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
