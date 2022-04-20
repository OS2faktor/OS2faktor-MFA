using System.Collections.Generic;
using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    class IdentifyClaim
    {
        [DataMember]
        public string type { get; set; }
        [DataMember]
        public string value { get; set; }
    }
}
