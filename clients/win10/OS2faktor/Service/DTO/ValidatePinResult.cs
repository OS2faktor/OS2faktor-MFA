namespace OS2faktor.Service.DTO
{
    public class ValidatePinResult
    {
        public string Status { get; set; } = "WRONG_PIN";
        public string LockedUntil { get; set; }
    }
}
