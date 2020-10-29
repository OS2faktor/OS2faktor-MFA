using System;

namespace OS2faktor.WebSockets
{
    public class PinResult
    {
        public PinResultStatus status;
        public string lockedUntil;
    }

    public enum PinResultStatus
    {
        WRONG_PIN, LOCKED
    }
}
