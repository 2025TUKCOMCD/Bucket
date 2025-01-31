
using System.Collections;
using UnityEngine;
using UnityEngine.Networking;

//REST API ��û �� ���� ó��
public class APIManager : MonoBehaviour
{
    private string serverURL = "http://localhost:8080/index.html";

    public void FetchWebPage()
    {
        StartCoroutine(GetWebContent()); //�񵿱� ��û
    }

    IEnumerator GetWebContent()
    {
        // serverURL���� HTML �����͸� ������
        UnityWebRequest request = UnityWebRequest.Get(serverURL); //GET ��û ����
        yield return request.SendWebRequest(); // ��û ���� �� ���� ���

        if(request.result == UnityWebRequest.Result.Success) //��û ���� ��
        {
            string WebContent = request.downloadHandler.text; // ���� ������ ����
            Debug.Log("Web Page Loaded: " + WebContent);
            //DisplayManager�� �����Ͽ� AR UI�� ǥ��(������ ����)
            DisplayManager.Instance.UpdateText(WebContent); 
        }
        else
        {
            Debug.LogError("Error: " + request.error);
        }
    }
    
}
