using System.Collections.Generic;
using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    class IdentifyResponse
    {
        [DataMember]
        public List<IdentifyResource> resources { get; set; }
    }
}
