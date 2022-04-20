using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    class IdentifyResource
    {
        [DataMember(Name = "urn:scim:schemas:extension:safewhere:identify:1.0")]
        public IdentifyRecord record { get; set; }
    }
}
