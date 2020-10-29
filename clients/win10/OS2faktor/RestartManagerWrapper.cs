using System.Runtime.InteropServices;

namespace OS2faktor
{
    class RestartManagerWrapper
    {
        [DllImport("kernel32.dll", CharSet = CharSet.Auto)]
        public static extern uint RegisterApplicationRestart(string pszCommandline, int dwFlags);
    }
}
