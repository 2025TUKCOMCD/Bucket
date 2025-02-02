LightWebView
============

LightWebView is a lightweight WebView plugin for Unity Android environment developed natively using Android. By integrating it into your Unity Android project, you can easily acquire the ability to open URLs (launch new activities).

### 1\. Plugin Components

*   **LightWebview/Plugins/Android**: Core plugin files
    *   `lightwebviewsdk-release.aar`: Android native WebView SDK.
    *   `lightwebviewunity-release.aar`: Unity API for lightwebviewsdk.
    *   `LightWebviewAndroid.cs`: C# script.

### 2\. Integration

*   Import `LightWebView_1.0.0.unitypackage`.
    
*   Configure Dependencies
    
    *   After enabling "Custom Main Gradle Template" and "Custom Gradle Properties Template" in Project Settings-Player-Publishing Settings-Build, `gradleTemplate.properties` and `mainTemplate.gradle` will be generated in the `Assets/Plugins/Android` directory.
    *   Add the following dependencies within the `dependencies` block of the `mainTemplate.gradle` file:
        
        java
        
        ```java
        implementation 'androidx.appcompat:appcompat:1.3.0'
        implementation 'com.google.android.material:material:1.4.0'
        implementation 'androidx.constraintlayout:constraintlayout:2.0.4'
        ```
        
*   Using the Plugin
    
    c#
    
    ```c
    // Open a URL without specifying a back mode (equivalent to passing LightWebviewAndroid.CloseMode.close).
    LightWebviewAndroid.instance.open(url);
    
    // Open a URL and specify the back mode:
    // LightWebviewAndroid.CloseMode.back: The back key performs the webpage's back operation.
    // LightWebviewAndroid.CloseMode.close: The back key performs the close operation.
    LightWebviewAndroid.instance.open("https://aios.soinluck.com/scene?sk=q842c2e079a1b32c8", LightWebviewAndroid.CloseMode.back);
    ```
