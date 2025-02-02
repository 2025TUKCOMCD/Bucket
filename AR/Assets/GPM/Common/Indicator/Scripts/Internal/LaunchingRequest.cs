using UnityEngine.Networking;

namespace Gpm.Common.Indicator.Internal
{
    public class LaunchingRequest
    {
        public UnityWebRequest RequestConfig()
        {
            string url = string.Format("{0}/main/release/{1}", Launching.URI, Launching.FILE_NAME);
            var request = UnityWebRequest.Get(url);
            request.method = UnityWebRequest.kHttpVerbGET;

            return request;
        }

        public UnityWebRequest RequestLaunchingInfo(Config config)
        {
            string appKey = config.launching.appKey;
            if (config.launching.isEncoding == true)
            {
                appKey = Decode(appKey);
            }

            string subKey = config.launching.subKey;

            string url = string.Format("{0}/{1}/appkeys/{2}/configurations", config.launching.url, config.launching.version, appKey);
            if (string.IsNullOrEmpty(subKey) == false)
            {
                url += "?subKey=launching." + subKey;
            }
            var request = UnityWebRequest.Get(url);
            request.method = UnityWebRequest.kHttpVerbGET;

            return request;
        }

        private string Decode(string encodedData)
        {
            try
            {
                byte[] bytes = System.Convert.FromBase64String(encodedData);
                string decodedData = System.Text.Encoding.UTF8.GetString(bytes);
                return decodedData;
            }
            catch (System.Exception ex)
            {
                UnityEngine.Debug.LogWarning(ex.Message);
                throw ex;
            }
        }
    }
}