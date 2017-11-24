## android studio 如何关联attach本framework层源码
* 进入XXX\platform_frameworks_base根目录,然后copy根目录下的core\java目录的地址。
* 打开C:\Users\XXX\.AndroidStudio3.0\config\options\jdk.table.xml
* 找到你想在android studio attach的目标版本 ，比如：Android API 26 Platform
* 替换里面的sourcePath
```
<sourcePath>
          <root type="composite">
            <root type="simple" url="file://F:/study/andoridSource/platform_frameworks_base/core/java" />
          </root>
        </sourcePath

```


* 完整版：

```
<jdk version="2">
      <name value="Android API 26 Platform" />
      <type value="Android SDK" />
      <homePath value="F:\developer\android\sdk" />
      <roots>
        <annotationsPath>
          <root type="composite">
            <root type="simple" url="jar://$APPLICATION_HOME_DIR$/plugins/android/lib/androidAnnotations.jar!/" />
          </root>
        </annotationsPath>
        <classPath>
          <root type="composite">
            <root type="simple" url="jar://F:/developer/android/sdk/platforms/android-26/android.jar!/" />
            <root type="simple" url="file://F:/developer/android/sdk/platforms/android-26/data/res" />
          </root>
        </classPath>
        <javadocPath>
          <root type="composite">
            <root type="simple" url="file://F:/developer/android/sdk/docs/reference" />
          </root>
        </javadocPath>
        <sourcePath>
          <root type="composite">
            <root type="simple" url="file://F:/study/andoridSource/platform_frameworks_base/core/java" />
          </root>
        </sourcePath>
      </roots>
      <additional jdk="1.8" sdk="android-26" />
    </jdk>
``