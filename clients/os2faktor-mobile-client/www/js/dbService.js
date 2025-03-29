/// list of keys in the database
/// ============================
/// regId                  - push token
/// name                   - actual name
/// deviceId               - actual deviceID
/// apiKey                 - actual ApiKey
/// isNemIdRegistered      - true if client is NemID registered
/// isPinCodeRegistered    - true if client is pin registered
/// isPin6                 - true if a 6 digit pin is used
/// pin                    - contains pincode for local validation
/// biometrics             - true if client has chosen to log in with biometrics

/// DBService
/// =========
/// Responsible for storing data in SQL databasen, and making it available for the
/// rest of the javascript code. Exposes methods for simple CRUD operations
function DBService() {
  var memDB = new Object();
  var sqlDB;

  this.init = function(handler) {
    if (browserOnly) {
      handler(true);
      return;
    }

    sqlDB = window.sqlitePlugin.openDatabase({
      name: 'os2f',
      location: 'default',
      androidDatabaseImplementation: 2
    });

    sqlDB.transaction(function(tx) {
      // ensure table exists
      tx.executeSql('CREATE TABLE IF NOT EXISTS settings (key unique, value)');

      // load everything from table into memory cache
      tx.executeSql('SELECT * FROM settings', [], function (tx, rs) {
        for (var i = 0; i < rs.rows.length; i++) {
          var key = rs.rows.item(i).key;
          var result = rs.rows.item(i).value;

          memDB[key] = result;
        }
      });
    }, function(error) {
      logService.logg("Database initialisering fejlede: " + error);

      handler(false);
    }, function() {
      logService.logg("Database initialisering afsluttet");

      handler(true);
    });
  }

  this.isSet = function(key) {
    return (memDB[key] != null);
  }

  this.setValue = function(key, value) {
    memDB[key] = String(value);

    if (browserOnly) {
      return;
    }

    sqlDB.transaction(function(tx) {
      tx.executeSql('SELECT count(*) AS antal FROM settings WHERE key = ?', [key], function(tx, rs) {
        if (rs.rows.item(0).antal > 0) {
          tx.executeSql('UPDATE settings SET value = ?1 WHERE key = ?2', [value, key], function(tx, rs) {
            ;
          }, function(tx, error) {
            logService.logg("Kan ikke opdatere " + key + " i databasen");
          });
        }
        else {
          tx.executeSql('INSERT INTO settings (key, value) VALUES (?1, ?2)', [key, value], function(tx, rs) {
            ;
          }, function(tx, error) {
            logService.logg("Kan ikke indsætte " + key + " i databasen");
          });
        }
      });
    });
  }

  this.getValue = function(key) {
    return memDB[key];
  }

  this.deleteValue = function(key) {
    memDB[key] = null;

    if (browserOnly) {
      return;
    }

    sqlDB.transaction(function(tx) {
      tx.executeSql('DELETE FROM settings WHERE key = ?', [key], function (tx, rs) {
        ;
      });
    }, function (error) {
      logService.logg("Kunne ikke slette " + key + " fra databasen");
    });
  }

  this.deleteAll = function() {
    dbService.deleteValue('name');
    dbService.deleteValue('apiKey');
    dbService.deleteValue('deviceId');
    dbService.deleteValue('isNemIdRegistered');
    dbService.deleteValue('isPinCodeRegistered');
    dbService.deleteValue('pin');
    dbService.deleteValue('isPin6');
    dbService.deleteValue('biometrics');
    dbService.deleteValue('isBlocked');
  }
}

