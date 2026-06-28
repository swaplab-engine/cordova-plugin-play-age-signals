var exec = require('cordova/exec');

/*
  Created by EMI INDO So on Jun 29, 2026
*/

var PlayAgeSignals = {
    /**
     * Requests age signals from the Google Play Age Signals API.
     * 
     * @param {Function} successCallback - Called when the API returns data successfully.
     * @param {Function} errorCallback - Called when the API encounters an error.
     */
    checkAgeSignals: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'PlayAgeSignalsPlugin', 'checkAgeSignals', []);
    }
};

module.exports = PlayAgeSignals;