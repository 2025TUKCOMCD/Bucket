using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class WebViewLoader : MonoBehaviour
{
    private string webPageURL = "http://localhost:8080/index.html";

    public void OpenWebPage()
    {
        Application.OpenURL(webPageURL);
        Debug.Log("버튼 테스트");
    }
}
