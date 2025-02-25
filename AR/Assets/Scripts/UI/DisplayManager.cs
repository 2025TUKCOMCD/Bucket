using System.Collections;
using System.Collections.Generic;
using TMPro; //TextMeshPro -> 텍스트 출력
using UnityEngine;

public class DisplayManager : MonoBehaviour
{
   
    public static DisplayManager Instance; // 인스턴스 생성
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
