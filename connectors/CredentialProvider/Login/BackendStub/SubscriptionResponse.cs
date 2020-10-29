using System.Runtime.Serialization;

namespace OS2faktorPlugin
{
    [DataContract]
    public class SubscriptionResponse
    {
        [DataMember]
        public string subscriptionKey { get; set; }

        [DataMember]
        public string pollingKey { get; set; }

        [DataMember]
        public bool clientNotified { get; set; }

        [DataMember]
        public bool clientAuthenticated { get; set; }

        [DataMember]
        public bool clientRejected { get; set; }

        [DataMember]
        public string challenge { get; set; }

        [DataMember]
        public string redirectUrl { get; set; }
    }
}
