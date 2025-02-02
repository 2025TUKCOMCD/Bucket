namespace Gpm.Common.Indicator.Internal
{
    public class Config
    {
        public class Launching
        {
            public string url;
            public string version;
            public string appKey;
            public string subKey;
            public bool isEncoding;
        }

        public Launching launching;

        public Config()
        {
            launching = new Launching();
        }
    }
}