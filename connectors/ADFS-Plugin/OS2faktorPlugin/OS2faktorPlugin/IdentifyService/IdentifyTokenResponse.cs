using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    class IdentifyTokenResponse
    {
        [DataMember]
        public string access_token { get; set; }

        [DataMember]
        public int expires_in { get; set; }
    }
}
