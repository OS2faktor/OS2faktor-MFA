using CefSharp;
using CefSharp.WinForms;
using System;
using System.DirectoryServices.AccountManagement;
using System.Windows.Forms;

namespace Login.Service
{
    class OS2Faktor
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        static OS2Faktor()
        {
            CefSettings settings = new CefSettings();
            settings.IgnoreCertificateErrors = true;

            // Initialize Cef with the provided settings
            Cef.Initialize(settings);
        }

        internal static DialogResult InitializeOS2faktorFlow(string username, string password)
        {
            if (ValidateCredentials(username, password))
            {
                LoginForm form = new LoginForm(username);
                return form.ShowDialog();
            }

            return DialogResult.Abort;
        }

        private static bool ValidateCredentials(string username, string password)
        {
            try
            {
                using (PrincipalContext principalContext = new PrincipalContext(ContextType.Domain))
                {
                    return principalContext.ValidateCredentials(username, password);
                }
            }
            catch (Exception ex)
            {
                log.Error("Validating user credentials: " + ex.Message, ex);
                return false;
            }
        }

    }
}
