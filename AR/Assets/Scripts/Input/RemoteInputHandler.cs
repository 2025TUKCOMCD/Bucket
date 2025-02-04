using UnityEngine;
using NRKernal;
using UnityEngine.SceneManagement;

public class RemoteInputHandler : MonoBehaviour
{
    /// <summary>
    /// XREAL Air�� ������ �Է��� �����Ͽ� ���� ��ȯ�մϴ�.
    /// </summary>
    public void Update()
    {
        // XREAL �������� "APP ��ư"�� ������ �� ����
        if (NRInput.GetButtonDown(ControllerButton.APP))
        {
            SceneManager.LoadScene("WebViewScene"); // ���� ������ �̵�
        }
    }
}
