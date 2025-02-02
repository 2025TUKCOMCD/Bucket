using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class WebViewGree : MonoBehaviour
{

    private WebViewObject webViewObject;
    public RectTransform webViewPanel;  //Panel�� ��ġ ���� ��������

    // Start is called before the first frame update
    void Start()
    {
        OpenWebPage();
    }

    public void OpenWebPage()
    {
        string strUrl = "https://www.naver.com";
        Debug.Log("������ �Ϸ�");
        try
        {
            if (webViewObject == null)
            {

                webViewObject = new GameObject("WebViewObject").AddComponent<WebViewObject>();
                webViewObject.Init((msg) =>
                {
                    Debug.Log(msg);
                });

                webViewObject.LoadURL(strUrl);
                webViewObject.SetVisibility(true);
                webViewObject.SetMargins(100, 400, 100, 300);
                Debug.Log("�̰͵� ���� �Ϸ�");
            }
            else
            {
                webViewObject.SetVisibility(true);
            }
        }
        catch (System.Exception e)
        {
            print($"WebView Error: {e}");
            Debug.Log("�̰� ���� ���� �Ϸ�");
        }
        /*//webViewObject ����
        webViewObject = new GameObject("WebViewObject").AddComponent<WebViewObject>();

        // WebView �ʱ�ȭ
        webViewObject.Init((msg) =>
        {
            Debug.Log("WebView Message: " + msg);
        });

        // Panel ũ�⿡ �°� WebView ũ�� ����
        float width = webViewPanel.rect.width;
        float height = webViewPanel.rect.height;
        webViewObject.SetMargins(0, 0, 0, 0);   //��ü ȭ��

        //�������� �ε�
        webViewObject.LoadURL("https://www.naver.com");
        webViewObject.SetVisibility(true);*/
    }

    // Update is called once per frame
    void Update()
    {

    }
}
