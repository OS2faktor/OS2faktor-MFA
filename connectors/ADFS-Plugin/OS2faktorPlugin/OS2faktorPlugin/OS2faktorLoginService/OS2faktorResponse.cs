using System.Collections.Generic;
using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    class OS2faktorResponse
    {
        [DataMember]
        public string cpr { get; set; }
        [DataMember]
        public string name { get; set; }
        [DataMember]
        public string email { get; set; }
        [DataMember]
        public string samAccountName { get; set; }

    }
}
