using System.Collections;
using System.Collections.Generic;
using TMPro; //TextMeshPro -> �ؽ�Ʈ ���
using UnityEngine;

public class DisplayManager : MonoBehaviour
{
   
    public static DisplayManager Instance; // �ν��Ͻ� ����
    public TextMeshProUGUI webTextDisplay;

    private void Awake()
    {
        if (Instance == null) { Instance = this; }
    }

    public void UpdateText(string text)
    {
        if(webTextDisplay != null)
        {
            webTextDisplay.text = text;
        }
    }
}
