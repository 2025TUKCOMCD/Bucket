namespace Gpm.WebView.Internal
{
    using System.Collections.Generic;
    using UnityEngine;

    public class DefaultWebView : IWebView
    {
        public bool CanGoBack => false;
        public bool CanGoForward => false;

        public void ShowUrl(string url,
            GpmWebViewRequest.Configuration configuration,
            GpmWebViewCallback.GpmWebViewDelegate callback,
            List<string> schemeList)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void ShowHtmlFile(
            string fileName,
            GpmWebViewRequest.Configuration configuration,
            GpmWebViewCallback.GpmWebViewDelegate callback,
            List<string> schemeList)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void ShowHtmlString(
            string source,
            GpmWebViewRequest.Configuration configuration,
            GpmWebViewCallback.GpmWebViewDelegate callback,
            List<string> schemeList)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void ShowSafeBrowsing(
            string url,
            GpmWebViewRequest.ConfigurationSafeBrowsing configuration = null,
            GpmWebViewCallback.GpmWebViewDelegate callback = null)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void CloseSafeBrowsing()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void Close()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public bool IsActive()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
            return false;
        }

        public void ExecuteJavaScript(string script)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void SetFileDownloadPath(string path)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void GoBack()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void GoForward()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void SetPosition(int x, int y)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void SetSize(int width, int height)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public void SetMargins(int left, int top, int right, int bottom)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }

        public int GetX()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
            return 0;
        }

        public int GetY()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
            return 0;
        }

        public int GetWidth()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
            return 0;
        }

        public int GetHeight()
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
            return 0;
        }

        public void ShowWebBrowser(string url)
        {
            Debug.LogWarning(GpmWebViewMessage.NOT_SUPPORTED_EDITOR);
        }
    }
}
