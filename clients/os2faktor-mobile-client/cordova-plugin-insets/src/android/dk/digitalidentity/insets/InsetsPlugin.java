package dk.digitalidentity.insets;

import org.apache.cordova.*;
import org.json.*;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public class InsetsPlugin extends CordovaPlugin {

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
    // Ensure edge-to-edge so insets are meaningful
    Activity a = cordova.getActivity();
    a.runOnUiThread(() -> WindowCompat.setDecorFitsSystemWindows(a.getWindow(), false));
  }

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext cb) {
    android.util.Log.d("InsetsPlugin", "execute(" + action + ")");
    if (!"getInsets".equals(action)) return false;

    final Activity a = cordova.getActivity();
    a.runOnUiThread(() -> {
      try {
        View root = a.getWindow().getDecorView();
        WindowInsetsCompat wi = ViewCompat.getRootWindowInsets(root);

        if (wi == null) {
          cb.error("Insets not available yet");
          return;
        }

        Insets status = wi.getInsets(WindowInsetsCompat.Type.statusBars());
        Insets nav    = wi.getInsets(WindowInsetsCompat.Type.navigationBars());
        Insets sys    = wi.getInsets(WindowInsetsCompat.Type.systemGestures());
        Insets ime    = wi.getInsets(WindowInsetsCompat.Type.ime());

        // Heuristic:
        // - Gesture nav => systemGestures.bottom > 0 and nav.bottom == 0 (when fully immersive)
        // - 3-button nav => nav.bottom > 0, systemGestures.bottom may be 0 or small
        boolean isGestureNav = (sys.bottom > 0) && (nav.bottom == 0);

        float density = cordova.getActivity().getResources().getDisplayMetrics().density;

        JSONObject out = new JSONObject();
        // out.put("top", status.top);
        // out.put("navBottom", nav.bottom);
        // out.put("systemGesturesBottom", sys.bottom);
        // out.put("imeBottom", ime.bottom);
        out.put("top", Math.round(status.top / density));
        out.put("navBottom", Math.round(nav.bottom / density));
        out.put("systemGesturesBottom", Math.round(sys.bottom / density));
        out.put("imeBottom", Math.round(ime.bottom / density));
        out.put("isGestureNav", isGestureNav);

        cb.success(out);
      } catch (Exception e) {
        cb.error(e.getMessage());
      }
    });

    return true;
  }
}
