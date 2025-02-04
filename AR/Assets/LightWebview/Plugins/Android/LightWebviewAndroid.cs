using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class LightWebviewAndroid
{
    private static LightWebviewAndroid _lightWebviewAndroid;

    private static AndroidJavaClass lightWebviewAndroid;

    private LightWebviewAndroid()
    {
        lightWebviewAndroid = new AndroidJavaClass("com.lightwebviewunity.UnityLightWebview");
    }

    public static LightWebviewAndroid instance
    {
        get
        {
            if (_lightWebviewAndroid == null)
            {
                _lightWebviewAndroid = new LightWebviewAndroid();
            }
            return _lightWebviewAndroid;
        }
    }

    public void open(string url) {
        open(url, CloseMode.close);
    }

    public void open(string url, CloseMode closeMode) {
        lightWebviewAndroid.CallStatic("open", url, (int)closeMode);
    }

    public enum CloseMode
    {
        close,    // 0
        back,    // 1
    }
}
