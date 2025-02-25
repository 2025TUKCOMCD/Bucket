using UnityEngine;
using NRKernal;
using UnityEngine.SceneManagement;

public class RemoteInputHandler : MonoBehaviour
{
    /// <summary>
    /// XREAL Air의 리모컨 입력을 감지하여 씬을 전환합니다.
    /// </summary>
    public void Update()
    {
        // XREAL 리모컨의 "APP 버튼"을 눌렀을 때 실행
        if (NRInput.GetButtonDown(ControllerButton.APP))
        {
            SceneManager.LoadScene("WebViewScene"); // 웹뷰 씬으로 이동
        }
    }
}
