namespace OS2faktor.Service.DTO
{
    public class StatusResult
    {
        private bool disabled = false;
        private bool lookupFailed = false;
        private bool pinProtected = false;
        private bool nemIdRegistered = false;

        public bool Disabled { get => disabled; set => disabled = value; }
        public bool LookupFailed { get => lookupFailed; set => lookupFailed = value; }
        public bool PinProtected { get => pinProtected; set => pinProtected = value; }
        public bool NemIdRegistered { get => nemIdRegistered; set => nemIdRegistered = value; }
    }
}