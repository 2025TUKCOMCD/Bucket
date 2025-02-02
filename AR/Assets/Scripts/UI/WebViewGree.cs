using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class WebViewGree : MonoBehaviour
{

    private WebViewObject webViewObject;
    public RectTransform webViewPanel;  //Panel의 위치 정보 가져오기

    // Start is called before the first frame update
    void Start()
    {
        OpenWebPage();
    }

    public void OpenWebPage()
    {
        string strUrl = "https://www.naver.com";
        Debug.Log("실행은 완료");
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
                Debug.Log("이것도 실행 완료");
            }
            else
            {
                webViewObject.SetVisibility(true);
            }
        }
        catch (System.Exception e)
        {
            print($"WebView Error: {e}");
            Debug.Log("이건 오류 실행 완료");
        }
        /*//webViewObject 생성
        webViewObject = new GameObject("WebViewObject").AddComponent<WebViewObject>();

        // WebView 초기화
        webViewObject.Init((msg) =>
        {
            Debug.Log("WebView Message: " + msg);
        });

        // Panel 크기에 맞게 WebView 크기 조정
        float width = webViewPanel.rect.width;
        float height = webViewPanel.rect.height;
        webViewObject.SetMargins(0, 0, 0, 0);   //전체 화면

        //웹페이지 로드
        webViewObject.LoadURL("https://www.naver.com");
        webViewObject.SetVisibility(true);*/
    }

    // Update is called once per frame
    void Update()
    {

    }
}
