# cordova-plugin-play-age-signals

[![NPM version](https://img.shields.io/npm/v/cordova-plugin-play-age-signals.svg)](https://www.npmjs.com/package/cordova-plugin-play-age-signals)
[![Downloads](https://img.shields.io/npm/dm/cordova-plugin-play-age-signals.svg)](https://www.npmjs.com/package/cordova-plugin-play-age-signals)
[![License](https://img.shields.io/npm/l/cordova-plugin-play-age-signals.svg)](https://github.com/swaplab-engine/cordova-plugin-play-age-signals/blob/main/LICENSE)

Cordova plugin to integrate the **Google Play Age Signals API (v0.0.6)** into web-based (HTML/JS/CSS) apps or games. Supported on Android 6.0 (API level 23) and higher.

Official Documentation: [Use Play Age Signals API](https://developer.android.com/google/play/age-signals/use-age-signals-api)

## Installation

Install directly from NPM:

```bash
cordova plugin add cordova-plugin-play-age-signals
```

## Basic Usage

Play Age Signals API v0.0.6 uses a 3-step workflow. Ensure the API is called only after the Cordova `deviceready` event fires:

1. **(Optional)** `getApplicableAgeFeatures()` — Check if mandatory age sharing applies to the user's region.
2. **(Required)** `requestAgeSignalsAccess()` — Request or check age sharing access (may show an in-app Play Store prompt if required).
3. `checkAgeSignals()` — Retrieve the actual age signals once `ageSignalsStatus` is `SHARED`.

```javascript
document.addEventListener('deviceready', onDeviceReady, false);

function onDeviceReady() {
    var PlayAgeSignals = cordova.plugins.PlayAgeSignals;

    // 1. (Optional) Check applicable age features
    PlayAgeSignals.getApplicableAgeFeatures(
        function(featuresResult) {
            console.log("Mandatory sharing:", featuresResult.isMandatorySharing);
        },
        function(error) {
            console.error("Features Error:", error);
        }
    );

    // 2. Request or check age signals access
    PlayAgeSignals.requestAgeSignalsAccess(
        function(accessResult) {
            if (accessResult.ageSignalsStatus === 'SHARED') {
                // 3. Retrieve age signals
                PlayAgeSignals.checkAgeSignals(
                    function(result) {
                        handleAgeSignals(result);
                    },
                    function(error) {
                        console.error("checkAgeSignals Error:", error);
                    }
                );
            } else if (accessResult.ageSignalsStatus === 'VERIFICATION_REQUIRED') {
                // User is in a mandatory region and must verify age in Google Play Store
                promptUserToVerifyInPlayStore();
            } else {
                // NOT_SHARED: User or parent declined to share age range
                handleUnsharedAge();
            }
        },
        function(error) {
            console.error("requestAgeSignalsAccess Error:", error);
        }
    );
}
```

## API Reference

### 1. `getApplicableAgeFeatures(successCallback, errorCallback)`
Returns a JSON object:
* `features` (Array of String): Contains `["MANDATORY_SHARING"]` for users in regulated regions with mandatory age sharing laws, or `[]` for all other users.
* `isMandatorySharing` (Boolean): Convenience flag (`true` if `MANDATORY_SHARING` is present).

### 2. `requestAgeSignalsAccess(successCallback, errorCallback)`
Returns a JSON object:
* `ageSignalsStatus` (String):
  * `SHARED`: User (or parent) agreed to share age range, or is in an eligible auto-share region. Proceed to `checkAgeSignals()`.
  * `NOT_SHARED`: User/parent declined to share age range.
  * `VERIFICATION_REQUIRED`: User's age is not verified in a region where age verification and sharing are mandatory. Ask the user to visit the Play Store to resolve their status.
* `ageSignalsStatusCode` (Number): Raw integer status code from the Play Age Signals SDK.

### 3. `checkAgeSignals(successCallback, errorCallback)`
Returns a JSON object. Values default to `null` if not applicable or not shared:
* `ageRangeSource` (String | null):
  * `TIER_A`: User provided their age information to Google (may or may not be confirmed).
  * `TIER_B`: User's age is managed by a parent or guardian (supervised account).
  * `TIER_C`: User's age is assessed via credit card, email, selfie assessment, Government ID, or Tax ID (regulated regions only).
  * `TIER_D`: User's age is verified via Government ID + selfie assessment, or Digital ID (regulated regions only).
* `ageLower` (Number | null): Inclusive lower bound of the user's age range (e.g., `0`, `13`, `16`, `18`).
* `ageUpper` (Number | null): Inclusive upper bound of the user's age range (e.g., `12`, `15`, `17`), or `null` for the highest open-ended band (e.g., `18+`).
* `installId` (String | null): Unique install ID assigned to supervised user installs by Google Play for revoked app approval notifications.
* `significantChangeStatus` (String | null): `APPROVED`, `PENDING`, `DECLINED`, or `null` (only non-null for supervised accounts with recorded significant changes in applicable jurisdictions).
* `significantChangeApprovalDate` (String | null): Effective date (`YYYY-MM-DD`, UTC) of the most recently approved significant change, or `null`.

### 4. `enableTestMode(options, successCallback, errorCallback)` & `disableTestMode(successCallback, errorCallback)`
Switches the internal manager to Google Play's `FakeAgeSignalsManager` so you can simulate any age signal, access status, or API error locally without Google Play.

> **Warning:** Never leave `enableTestMode()` active in production/release builds.

```javascript
// Simulate a supervised minor (13-15) with an approved significant change
cordova.plugins.PlayAgeSignals.enableTestMode({
    mandatorySharing: true,
    ageSignalsStatus: 'SHARED',
    ageRangeSource: 'TIER_B',
    ageLower: 13,
    ageUpper: 15,
    installId: 'fake_install_id_123',
    significantChangeStatus: 'APPROVED',
    significantChangeApprovalDate: 1777939200000 // UTC timestamp (e.g. 2026-05-01)
}, function() {
    // Call getApplicableAgeFeatures / requestAgeSignalsAccess / checkAgeSignals
});

// Simulate an API error (e.g. NETWORK_ERROR = -3)
cordova.plugins.PlayAgeSignals.enableTestMode({
    errorCode: -3
});

// Switch back to live Google Play production manager
cordova.plugins.PlayAgeSignals.disableTestMode();
```

### Error Object
All `errorCallback` functions return a JSON object:

```json
{
    "code": -3,
    "message": "Network error description..."
}
```

Common retryable error codes: `-1` (`API_NOT_AVAILABLE`), `-2` (`PLAY_STORE_NOT_FOUND`), `-3` (`NETWORK_ERROR`), `-4` (`PLAY_SERVICES_NOT_FOUND`), `-5` (`CANNOT_BIND_TO_SERVICE`), `-6` (`PLAY_STORE_VERSION_OUTDATED`), `-7` (`PLAY_SERVICES_VERSION_OUTDATED`), `-8` (`CLIENT_TRANSIENT_ERROR`).

Non-retryable error codes: `-9` (`APP_NOT_OWNED`), `-10` (`SDK_VERSION_OUTDATED`), `-11` (`DEVICE_NOT_SUPPORTED`), `-100` (`INTERNAL_ERROR`).

## Application-Side Handling (Action Required)

The plugin only retrieves signals. Developers must implement UI and logic restrictions based on the returned signals to comply with applicable regulations.

### 1. Significant Change Denial or Pending Approval
For supervised users (`ageRangeSource === 'TIER_B'`), check `significantChangeStatus`:
* **Condition:** `significantChangeStatus` is `DECLINED` or `PENDING`.
* **Action:** Restrict access to features covered by the significant change until parental approval is granted (`APPROVED`).

### 2. Content or Feature Restriction
* **Condition:** Age range (`ageLower` and `ageUpper`) indicates a minor (e.g., `0–12` or `13–17`), or `ageRangeSource === 'TIER_B'`.
* **Action:** Disable public chat, hide store/top-up menus, require parental approval, or activate a safe mode content filter.

### 3. Disable Tracking and Targeted Ads (Privacy & Ads Compliance)
* **Condition:** Age range indicates the user is a child.
* **Action:** Configure analytics SDKs to avoid collecting Advertising IDs, and configure Ad SDKs (e.g., AdMob) to serve only non-personalized ads.

### Example Implementation

```javascript
function handleAgeSignals(result) {
    // Handle supervised significant changes
    if (result.significantChangeStatus === 'DECLINED') {
        blockRestrictedFeatures("Significant change declined by parent or guardian.");
        return;
    }

    if (result.significantChangeStatus === 'PENDING') {
        restrictNewFeaturesUntilApproved();
    }

    // Handle age bands
    if (result.ageLower !== null) {
        if (result.ageUpper !== null && result.ageUpper <= 12) {
            // Child (0-12)
            disableTargetedAdvertising();
            disableInAppPurchases();
            hidePublicChatBox();
            enableKidsSafeMode();
        } else if (result.ageLower >= 13 && result.ageUpper !== null && result.ageUpper <= 17) {
            // Teen (13-17)
            enableTeenMode();
            requireParentalPinForPurchases();
        } else if (result.ageLower >= 18 && result.ageUpper === null) {
            // Adult (18+)
            enableFullAppExperience();
        }
    } else {
        // Both bounds are null (age not shared)
        enableDefaultExperience();
    }
}
```

### Example Implementation AdMob

If your app uses [cordova-plugin-admob-nextgen](https://github.com/swaplab-engine/cordova-plugin-admob-nextgen), keep both plugins decoupled and map the age signals to AdMob's request configuration (`tagForChildDirectedTreatment`, `tagForUnderAgeOfConsent`, `maxAdContentRating`) **before** initializing or loading ads:

```javascript
function getAdMobConfigFromAgeSignals(signals) {
    if (signals.ageLower !== null) {
        if (signals.ageUpper !== null && signals.ageUpper <= 12) {
            // Child (0-12)
            return {
                tagForChildDirectedTreatment: true,
                tagForUnderAgeOfConsent: true,
                maxAdContentRating: 'G'
            };
        } else if (signals.ageLower >= 13 && signals.ageUpper !== null && signals.ageUpper <= 17) {
            // Teen (13-17)
            return {
                tagForChildDirectedTreatment: false,
                tagForUnderAgeOfConsent: true,
                maxAdContentRating: 'T'
            };
        } else if (signals.ageLower >= 18 && signals.ageUpper === null) {
            // Adult (18+)
            return {
                tagForChildDirectedTreatment: false,
                tagForUnderAgeOfConsent: false,
                maxAdContentRating: 'MA'
            };
        }
    }

    // Fallback when age is not shared
    return {
        tagForChildDirectedTreatment: false,
        tagForUnderAgeOfConsent: false,
        maxAdContentRating: 'PG'
    };
}

// Usage in deviceready:
cordova.plugins.PlayAgeSignals.requestAgeSignalsAccess(function(accessResult) {
    if (accessResult.ageSignalsStatus === 'SHARED') {
        cordova.plugins.PlayAgeSignals.checkAgeSignals(function(signals) {
            var adConfig = getAdMobConfigFromAgeSignals(signals);
            // Apply adConfig to cordova-plugin-admob-nextgen before initializing/loading ads
            console.log("AdMob Config:", adConfig);
        });
    }
});
```

## License

MIT