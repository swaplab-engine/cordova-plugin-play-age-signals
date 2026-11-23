var exec = require('cordova/exec');

/*
  Created by EMI INDO So on Jun 29, 2026
  Updated for Play Age Signals API v0.0.6
*/

var PlayAgeSignals = {
    /**
     * Enables test mode using FakeAgeSignalsManager to simulate API responses without Google Play.
     * DO NOT leave enabled in production builds.
     *
     * @param {Object} options - Mock configuration (mandatorySharing, ageSignalsStatus, ageRangeSource, ageLower, ageUpper, installId, significantChangeStatus, significantChangeApprovalDate, errorCode)
     * @param {Function} successCallback
     * @param {Function} errorCallback
     */
    enableTestMode: function(options, successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'PlayAgeSignalsPlugin', 'enableTestMode', [options || {}]);
    },

    /**
     * Disables test mode and restores the production AgeSignalsManager.
     */
    disableTestMode: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'PlayAgeSignalsPlugin', 'disableTestMode', []);
    },
    
    /**
     * (Optional) Checks which age-related features apply to the user (e.g., MANDATORY_SHARING).
     *
     * @param {Function} successCallback - Returns { features: string[], isMandatorySharing: boolean }
     * @param {Function} errorCallback - Returns { code: number, message: string }
     */
    getApplicableAgeFeatures: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'PlayAgeSignalsPlugin', 'getApplicableAgeFeatures', []);
    },

    /**
     * Requests or checks age signals access. May display an in-app Play Store prompt if required.
     *
     * @param {Function} successCallback - Returns { ageSignalsStatus: 'SHARED' | 'NOT_SHARED' | 'VERIFICATION_REQUIRED', ageSignalsStatusCode: number }
     * @param {Function} errorCallback - Returns { code: number, message: string }
     */
    requestAgeSignalsAccess: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'PlayAgeSignalsPlugin', 'requestAgeSignalsAccess', []);
    },

    /**
     * Retrieves age signals once ageSignalsStatus is 'SHARED'.
     *
     * @param {Function} successCallback - Returns { ageRangeSource, ageLower, ageUpper, installId, significantChangeStatus, significantChangeApprovalDate }
     * @param {Function} errorCallback - Returns { code: number, message: string }
     */
    checkAgeSignals: function(successCallback, errorCallback) {
        exec(successCallback, errorCallback, 'PlayAgeSignalsPlugin', 'checkAgeSignals', []);
    }
};

module.exports = PlayAgeSignals;