using UnityEngine;

namespace Gpm.WebView.Internal
{
    public class AndroidWebView : NativeWebView
    {
        private const string ANDROID_CLASS_NAME = "com.gpm.webviewplugin.GpmWebViewPlugin";

        protected override void Initialize()
        {
            CLASS_NAME = ANDROID_CLASS_NAME;
            base.Initialize();
        }

        public override void CloseSafeBrowsing()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_ANDROID);
        }
    }
}
