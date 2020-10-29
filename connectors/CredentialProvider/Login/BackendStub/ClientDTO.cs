using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    public class ClientDTO
    {
        [DataMember]
        public string deviceId { get; set; }

        [DataMember]
        public string type { get; set; }

        [DataMember]
        public string name { get; set; }
    }
}
