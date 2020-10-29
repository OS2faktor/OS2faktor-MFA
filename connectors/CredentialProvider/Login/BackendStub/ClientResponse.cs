using System.Collections.Generic;
using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    public class ClientResponse
    {
        [DataMember]
        public List<ClientDTO> clients { get; set; }
    }
}
