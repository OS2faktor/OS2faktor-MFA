using System.Collections.Generic;
using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    class IdentifyRecord
    {
        [DataMember]
        public List<IdentifyClaim> claims { get; set; }
    }
}
