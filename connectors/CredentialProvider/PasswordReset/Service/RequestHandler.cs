using CefSharp;
using CefSharp.Handler;
using System;
using System.Collections.Generic;

namespace PasswordReset.Service
{
    public class RequestHandler : DefaultRequestHandler
    {
        private static log4net.ILog log = log4net.LogManager.GetLogger(System.Reflection.MethodBase.GetCurrentMethod().DeclaringType);
        List<string> allowedUrls = Config.GetAllowedUrls();

        public override bool OnBeforeBrowse(IWebBrowser browserControl, IBrowser browser, IFrame frame, IRequest request, bool userGesture, bool isRedirect)
        {
            Uri uri = new Uri(request.Url);
            if (!allowedUrls.Contains(uri.Authority))
            {
                log.Warn("Blocking access to: " + uri.Authority);
                return true;
            }

            log.Debug("Allowing access to: " + uri.Authority);

            return false;
        }
    }
}