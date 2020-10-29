using CefSharp;
using CefSharp.WinForms;
using System;
using System.DirectoryServices;
using System.DirectoryServices.AccountManagement;
using System.Windows.Forms;

namespace PasswordReset.Service
{
    class OS2Faktor
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);

        static OS2Faktor(){
            CefSettings settings = new CefSettings();
            settings.IgnoreCertificateErrors = true;

            // Initialize Cef with the provided settings
            Cef.Initialize(settings);
        }

        public void CefShutdown()
        {
            Cef.Shutdown();
        }

        public static object InitializeResetPasswordFlow()
        {
            try
            {
                ResetForm form = new ResetForm();
                var dialogresult = form.ShowDialog();

                dynamic result = new System.Dynamic.ExpandoObject();
                result.DialogResult = dialogresult;

                //Successfuly changed password
                if (DialogResult.OK.Equals(dialogresult))
                {
                    result.Username = form.GetUsername();
                    result.Password = form.GetPassword();
                }

                return result;
            }
            catch (Exception ex)
            {
                log.Error("Error:", ex);
            }

            return null;
        }
    }
}
