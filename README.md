## android studio ��ι���attach��framework��Դ��
* ����XXX\platform_frameworks_base��Ŀ¼,Ȼ��copy��Ŀ¼�µ�core\javaĿ¼�ĵ�ַ��
* ��C:\Users\XXX\.AndroidStudio3.0\config\options\jdk.table.xml
* �ҵ�������android studio attach��Ŀ��汾 �����磺Android API 26 Platform
* �滻�����sourcePath
```
<sourcePath>
          <root type="composite">
            <root type="simple" url="file://F:/study/andoridSource/platform_frameworks_base/core/java" />
          </root>
        </sourcePath

```


* �����棺

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