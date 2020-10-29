using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    class CprServiceResponse
    {
        [DataMember]
        public string result { get; set; }
    }
}
