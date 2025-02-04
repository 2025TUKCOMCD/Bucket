
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;

//REST API 요청 및 응답 처리
public class APIManager : MonoBehaviour
{
    private string serverURL = "http://localhost:8080/index.html";

    public void FetchWebPage()
    {
        StartCoroutine(GetWebContent()); //비동기 요청
    }

    IEnumerator GetWebContent()
    {
        // serverURL에서 HTML 데이터를 가져옴
        UnityWebRequest request = UnityWebRequest.Get(serverURL); //GET 요청 생성
        yield return request.SendWebRequest(); // 요청 실행 및 응답 대기

        if(request.result == UnityWebRequest.Result.Success) //요청 성공 시
        {
            string WebContent = request.downloadHandler.text; // 받은 데이터 저장
            Debug.Log("Web Page Loaded: " + WebContent);
            //DisplayManager에 전달하여 AR UI에 표시(데이터 전달)
            DisplayManager.Instance.UpdateText(WebContent); 
        }
        else
        {
            Debug.LogError("Error: " + request.error);
        }
    }
    
}
