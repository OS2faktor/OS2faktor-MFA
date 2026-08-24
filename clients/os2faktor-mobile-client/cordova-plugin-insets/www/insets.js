var exec = require('cordova/exec');

var Insets = {
  /**
   * Get current insets and derived flags.
   * Logs the raw result and any errors.
   */
  getInsets: function () {
    return new Promise(function (resolve, reject) {
      console.log('[InsetsPlugin] Calling native getInsets…');
      exec(
        function (res) {
          // console.log('[InsetsPlugin] Success:', JSON.stringify(res));
          resolve(res);
        },
        function (err) {
          // console.error('[InsetsPlugin] Error:', err);
          reject(err);
        },
        'InsetsPlugin',
        'getInsets',
        []
      );
    }).then(function (ins) {
      // Enrich with convenient fields
      var bottomSafe = Math.max(ins.systemGesturesBottom || 0, ins.navBottom || 0);
      var effectiveBottom = Math.max(bottomSafe, ins.imeBottom || 0);
      ins.bottomSafe = bottomSafe;
      ins.effectiveBottom = effectiveBottom;
      // console.log('[InsetsPlugin] Enriched result:', JSON.stringify(ins));
      return ins;
    });
  },

  /**
   * Apply CSS variables to :root for quick layout.
   * Logs what values it sets.
   */
  applyCssVars: function () {
    return this.getInsets().then(function (ins) {
      var r = document.documentElement.style;
      r.setProperty('--safe-top', (ins.top || 0) + 'px');
      r.setProperty('--safe-bottom', (ins.bottomSafe || 0) + 'px');
      r.setProperty('--effective-bottom', (ins.effectiveBottom || 0) + 'px');
      // console.log('[InsetsPlugin] Applied CSS vars:', JSON.stringify({
      //   safeTop: ins.top,
      //   safeBottom: ins.bottomSafe,
      //   effectiveBottom: ins.effectiveBottom
      // }));
      return ins;
    });
  }
};

module.exports = Insets;
