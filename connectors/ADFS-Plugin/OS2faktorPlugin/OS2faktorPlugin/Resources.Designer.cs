﻿//------------------------------------------------------------------------------
// <auto-generated>
//     This code was generated by a tool.
//     Runtime Version:4.0.30319.42000
//
//     Changes to this file may cause incorrect behavior and will be lost if
//     the code is regenerated.
// </auto-generated>
//------------------------------------------------------------------------------

namespace OS2faktorPlugin {
    using System;
    
    
    /// <summary>
    ///   A strongly-typed resource class, for looking up localized strings, etc.
    /// </summary>
    // This class was auto-generated by the StronglyTypedResourceBuilder
    // class via a tool like ResGen or Visual Studio.
    // To add or remove a member, edit your .ResX file then rerun ResGen
    // with the /str option, or rebuild your VS project.
    [global::System.CodeDom.Compiler.GeneratedCodeAttribute("System.Resources.Tools.StronglyTypedResourceBuilder", "15.0.0.0")]
    [global::System.Diagnostics.DebuggerNonUserCodeAttribute()]
    [global::System.Runtime.CompilerServices.CompilerGeneratedAttribute()]
    internal class Resources {
        
        private static global::System.Resources.ResourceManager resourceMan;
        
        private static global::System.Globalization.CultureInfo resourceCulture;
        
        [global::System.Diagnostics.CodeAnalysis.SuppressMessageAttribute("Microsoft.Performance", "CA1811:AvoidUncalledPrivateCode")]
        internal Resources() {
        }
        
        /// <summary>
        ///   Returns the cached ResourceManager instance used by this class.
        /// </summary>
        [global::System.ComponentModel.EditorBrowsableAttribute(global::System.ComponentModel.EditorBrowsableState.Advanced)]
        internal static global::System.Resources.ResourceManager ResourceManager {
            get {
                if (object.ReferenceEquals(resourceMan, null)) {
                    global::System.Resources.ResourceManager temp = new global::System.Resources.ResourceManager("OS2faktorPlugin.Resources", typeof(Resources).Assembly);
                    resourceMan = temp;
                }
                return resourceMan;
            }
        }
        
        /// <summary>
        ///   Overrides the current thread's CurrentUICulture property for all
        ///   resource lookups using this strongly typed resource class.
        /// </summary>
        [global::System.ComponentModel.EditorBrowsableAttribute(global::System.ComponentModel.EditorBrowsableState.Advanced)]
        internal static global::System.Globalization.CultureInfo Culture {
            get {
                return resourceCulture;
            }
            set {
                resourceCulture = value;
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to &lt;div id=&quot;loginArea&quot;&gt;
        ///	&lt;form method=&quot;post&quot; id=&quot;loginForm&quot;&gt;
        ///		&lt;input id=&quot;authMethod&quot; type=&quot;hidden&quot; name=&quot;AuthMethod&quot; value=&quot;%AuthMethod%&quot; /&gt; &lt;input id=&quot;context&quot; type=&quot;hidden&quot; name=&quot;Context&quot; value=&quot;%Context%&quot; /&gt;
        ///
        ///		&lt;div class=&quot;fieldMargin bigText&quot;&gt;Der er opstået en teknisk fejl!&lt;/div&gt;
        ///        &lt;div id=&quot;pageIntroductionText&quot;&gt;
        ///
        ///            Der er opstået en teknisk fejl. Forsøg at logge på forfra, og hvis fejlen opstår igen,
        ///            skal du kontakte din administrator.
        ///        &lt;/div&gt;
        ///	&lt;/form&gt;
        ///&lt;/div&gt;.
        /// </summary>
        internal static string OS2faktorErrorForm {
            get {
                return ResourceManager.GetString("OS2faktorErrorForm", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to &lt;div id=&quot;loginArea&quot;&gt;
        ///	&lt;form method=&quot;post&quot; id=&quot;loginForm&quot;&gt;
        ///		&lt;input id=&quot;authMethod&quot; type=&quot;hidden&quot; name=&quot;AuthMethod&quot; value=&quot;%AuthMethod%&quot; /&gt; &lt;input id=&quot;context&quot; type=&quot;hidden&quot; name=&quot;Context&quot; value=&quot;%Context%&quot; /&gt;
        ///
        ///		&lt;div class=&quot;fieldMargin bigText&quot;&gt;OS2faktor Login&lt;/div&gt;
        ///
        ///        &lt;div id=&quot;pageIntroductionText&quot;&gt;
        ///			&lt;span id=&quot;outside-browser&quot; style=&quot;display: none;&quot;&gt;
        ///				&lt;p&gt;
        ///					Din OS2faktor klient åbner om lidt og spørger om du vil tillade login,
        ///					når dette sker, skal du verificere at den kode der  [rest of string was truncated]&quot;;.
        /// </summary>
        internal static string OS2faktorLoginForm {
            get {
                return ResourceManager.GetString("OS2faktorLoginForm", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to &lt;div id=&quot;loginArea&quot;&gt;
        ///	&lt;form method=&quot;post&quot; id=&quot;loginForm&quot;&gt;
        ///		&lt;input id=&quot;authMethod&quot; type=&quot;hidden&quot; name=&quot;AuthMethod&quot; value=&quot;%AuthMethod%&quot; /&gt; &lt;input id=&quot;context&quot; type=&quot;hidden&quot; name=&quot;Context&quot; value=&quot;%Context%&quot; /&gt;
        ///
        ///		&lt;div class=&quot;fieldMargin bigText&quot;&gt;Ingen registrerede OS2faktor klienter&lt;/div&gt;
        ///        &lt;div id=&quot;pageIntroductionText&quot;&gt;
        ///            &lt;p&gt;For at gennemføre login, skal du bruge en OS2faktor klient.&lt;/p&gt;
        ///            &lt;br/&gt;
        ///
        ///            &lt;p&gt;
        ///                Du kan læse mere om hvordan du får en OS2fa [rest of string was truncated]&quot;;.
        /// </summary>
        internal static string OS2faktorNoDevicesForm {
            get {
                return ResourceManager.GetString("OS2faktorNoDevicesForm", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to &lt;div id=&quot;loginArea&quot;&gt;
        ///	&lt;form method=&quot;post&quot; id=&quot;loginForm&quot;&gt;
        ///		&lt;input id=&quot;authMethod&quot; type=&quot;hidden&quot; name=&quot;AuthMethod&quot; value=&quot;%AuthMethod%&quot; /&gt; &lt;input id=&quot;context&quot; type=&quot;hidden&quot; name=&quot;Context&quot; value=&quot;%Context%&quot; /&gt;
        ///
        ///		&lt;div class=&quot;fieldMargin bigText&quot;&gt;OS2faktor Login&lt;/div&gt;
        ///        &lt;div id=&quot;selectClientText&quot;&gt;
        ///
        ///			&lt;link rel=&quot;stylesheet&quot; href=&quot;https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css&quot; /&gt;
        ///			&lt;style&gt;
        ///				a.clientlink:hover { text-decoration: underline; font-weight: bol [rest of string was truncated]&quot;;.
        /// </summary>
        internal static string OS2faktorPickDeviceForm {
            get {
                return ResourceManager.GetString("OS2faktorPickDeviceForm", resourceCulture);
            }
        }
        
        /// <summary>
        ///   Looks up a localized string similar to &lt;div id=&quot;loginArea&quot;&gt;
        ///	&lt;form method=&quot;post&quot; id=&quot;loginForm&quot;&gt;
        ///		&lt;input id=&quot;authMethod&quot; type=&quot;hidden&quot; name=&quot;AuthMethod&quot; value=&quot;%AuthMethod%&quot; /&gt; &lt;input id=&quot;context&quot; type=&quot;hidden&quot; name=&quot;Context&quot; value=&quot;%Context%&quot; /&gt;
        ///
        ///		&lt;div class=&quot;fieldMargin bigText&quot;&gt;OS2faktor afvisning!&lt;/div&gt;
        ///        &lt;div id=&quot;pageIntroductionText&quot;&gt;
        ///            Du har ikke godkendt login på din OS2faktor enhed, så login er blevet afbrudt. Prøv at logge på igen, og
        ///            godkend login forsøget på din OS2faktor enhed.
        ///        &lt;/div&gt;
        /// [rest of string was truncated]&quot;;.
        /// </summary>
        internal static string OS2faktorRejectedForm {
            get {
                return ResourceManager.GetString("OS2faktorRejectedForm", resourceCulture);
            }
        }
    }
}