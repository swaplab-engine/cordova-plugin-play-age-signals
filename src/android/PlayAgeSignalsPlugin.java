package cordova.plugin.playagesignals;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.play.agesignals.AgeSignalsAccessRequest;
import com.google.android.play.agesignals.AgeSignalsAccessResult;
import com.google.android.play.agesignals.AgeSignalsException;
import com.google.android.play.agesignals.AgeSignalsManager;
import com.google.android.play.agesignals.AgeSignalsManagerFactory;
import com.google.android.play.agesignals.AgeSignalsRequest;
import com.google.android.play.agesignals.AgeSignalsResult;
import com.google.android.play.agesignals.ApplicableAgeFeaturesRequest;
import com.google.android.play.agesignals.ApplicableAgeFeaturesResult;
import com.google.android.play.agesignals.model.AgeRangeSource;
import com.google.android.play.agesignals.model.AgeSignalsStatus;
import com.google.android.play.agesignals.model.ApplicableAgeFeature;
import com.google.android.play.agesignals.model.SignificantChangeStatus;
import com.google.android.play.agesignals.testing.FakeAgeSignalsManager;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

public class PlayAgeSignalsPlugin extends CordovaPlugin {

    private static final String TAG = "PlayAgeSignalsPlugin";

    private static final String ACTION_GET_APPLICABLE_AGE_FEATURES = "getApplicableAgeFeatures";
    private static final String ACTION_REQUEST_AGE_SIGNALS_ACCESS = "requestAgeSignalsAccess";
    private static final String ACTION_CHECK_AGE_SIGNALS = "checkAgeSignals";
    private static final String ACTION_ENABLE_TEST_MODE = "enableTestMode";
    private static final String ACTION_DISABLE_TEST_MODE = "disableTestMode";

    private AgeSignalsManager ageSignalsManager;

    @Override
    protected void pluginInitialize() {
        super.pluginInitialize();
        Context context = this.cordova.getActivity().getApplicationContext();
        this.ageSignalsManager = AgeSignalsManagerFactory.create(context);
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (ACTION_ENABLE_TEST_MODE.equals(action)) {
            JSONObject options = args.optJSONObject(0);
            this.cordova.getThreadPool().execute(() -> enableTestMode(options, callbackContext));
            return true;
        }

        if (ACTION_DISABLE_TEST_MODE.equals(action)) {
            this.cordova.getThreadPool().execute(() -> disableTestMode(callbackContext));
            return true;
        }

        if (ACTION_GET_APPLICABLE_AGE_FEATURES.equals(action)) {
            this.cordova.getThreadPool().execute(() -> getApplicableAgeFeatures(callbackContext));
            return true;
        }

        if (ACTION_REQUEST_AGE_SIGNALS_ACCESS.equals(action)) {

            this.cordova.getActivity().runOnUiThread(() -> requestAgeSignalsAccess(callbackContext));
            return true;
        }

        if (ACTION_CHECK_AGE_SIGNALS.equals(action)) {
            this.cordova.getThreadPool().execute(() -> checkAgeSignals(callbackContext));
            return true;
        }

        return false;
    }

    private void enableTestMode(JSONObject options, final CallbackContext callbackContext) {
        try {
            FakeAgeSignalsManager fakeManager = new FakeAgeSignalsManager();

            if (options != null) {

                if (options.has("errorCode") && !options.isNull("errorCode")) {
                    int errorCode = options.getInt("errorCode");
                    fakeManager.setNextAgeSignalsException(new AgeSignalsException(errorCode));
                } else {

                    boolean mandatorySharing = options.optBoolean("mandatorySharing", false);
                    ApplicableAgeFeaturesResult fakeFeatures = ApplicableAgeFeaturesResult.builder()
                            .setFeatures(mandatorySharing
                                    ? Collections.singleton(ApplicableAgeFeature.MANDATORY_SHARING)
                                    : Collections.emptySet())
                            .build();
                    fakeManager.setNextApplicableAgeFeaturesResult(fakeFeatures);

                    String accessStatusStr = options.optString("ageSignalsStatus", "SHARED");
                    int accessStatus = parseAgeSignalsStatus(accessStatusStr);
                    AgeSignalsAccessResult fakeAccess = AgeSignalsAccessResult.builder()
                            .setAgeSignalsStatus(accessStatus)
                            .build();
                    fakeManager.setNextAgeSignalsAccessResult(fakeAccess);

                    AgeSignalsResult.Builder resultBuilder = AgeSignalsResult.builder();

                    if (options.has("ageRangeSource") && !options.isNull("ageRangeSource")) {
                        Integer source = parseAgeRangeSource(options.getString("ageRangeSource"));
                        if (source != null) {
                            resultBuilder.setAgeRangeSource(source);
                        }
                    }

                    if (options.has("ageLower") && !options.isNull("ageLower")) {
                        resultBuilder.setAgeLower(options.getInt("ageLower"));
                    }

                    if (options.has("ageUpper") && !options.isNull("ageUpper")) {
                        resultBuilder.setAgeUpper(options.getInt("ageUpper"));
                    }

                    if (options.has("installId") && !options.isNull("installId")) {
                        resultBuilder.setInstallId(options.getString("installId"));
                    }

                    if (options.has("significantChangeStatus") && !options.isNull("significantChangeStatus")) {
                        Integer sigStatus = parseSignificantChangeStatus(options.getString("significantChangeStatus"));
                        if (sigStatus != null) {
                            resultBuilder.setSignificantChangeStatus(sigStatus);
                        }
                    }

                    if (options.has("significantChangeApprovalDate") && !options.isNull("significantChangeApprovalDate")) {
                        long timestamp = options.optLong("significantChangeApprovalDate", -1L);
                        if (timestamp > 0) {
                            resultBuilder.setSignificantChangeApprovalDate(new Date(timestamp));
                        }
                    }

                    fakeManager.setNextAgeSignalsResult(resultBuilder.build());
                }
            }

            this.ageSignalsManager = fakeManager;

            callbackContext.success("TEST_MODE_ENABLED");
        } catch (Exception e) {
            callbackContext.error("TEST_MODE_ERROR: " + e.getMessage());
        }
    }

    private void disableTestMode(final CallbackContext callbackContext) {
        try {
            Context context = this.cordova.getActivity().getApplicationContext();
            this.ageSignalsManager = AgeSignalsManagerFactory.create(context);

            callbackContext.success("TEST_MODE_DISABLED");
        } catch (Exception e) {
            callbackContext.error("INITIALIZATION_ERROR: " + e.getMessage());
        }
    }

    private void getApplicableAgeFeatures(final CallbackContext callbackContext) {
        try {
            ageSignalsManager.getApplicableAgeFeatures(ApplicableAgeFeaturesRequest.builder().build())
                    .addOnSuccessListener(featuresResult -> {
                        try {
                            JSONObject resultObj = new JSONObject();
                            JSONArray featuresArray = new JSONArray();
                            Set<Integer> features = featuresResult.features();

                            boolean mandatorySharing = false;
                            if (features != null) {
                                for (Integer feature : features) {
                                    if (feature != null && feature == ApplicableAgeFeature.MANDATORY_SHARING) {
                                        featuresArray.put("MANDATORY_SHARING");
                                        mandatorySharing = true;
                                    }
                                }
                            }

                            resultObj.put("features", featuresArray);
                            resultObj.put("isMandatorySharing", mandatorySharing);
                                callbackContext.success(resultObj);
                        } catch (JSONException e) {
                            callbackContext.error("JSON_PARSING_ERROR: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> sendError(callbackContext, e));
        } catch (Exception e) {
            callbackContext.error("INITIALIZATION_ERROR: " + e.getMessage());
        }
    }

    private void requestAgeSignalsAccess(final CallbackContext callbackContext) {
        Activity activity = this.cordova.getActivity();
        if (activity == null) {
            callbackContext.error("ACTIVITY_NOT_AVAILABLE");
            return;
        }

        try {
            AgeSignalsAccessRequest accessRequest = AgeSignalsAccessRequest.builder()
                    .setActivity(activity)
                    .build();

            ageSignalsManager.requestAgeSignalsAccess(accessRequest)
                    .addOnSuccessListener(accessResult -> {
                        try {
                            JSONObject resultObj = new JSONObject();
                            Integer status = accessResult.ageSignalsStatus();
                            resultObj.put("ageSignalsStatus", mapAgeSignalsStatus(status));
                            resultObj.putOpt("ageSignalsStatusCode", status);
                            callbackContext.success(resultObj);
                        } catch (JSONException e) {
                            callbackContext.error("JSON_PARSING_ERROR: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> sendError(callbackContext, e));
        } catch (Exception e) {
            callbackContext.error("INITIALIZATION_ERROR: " + e.getMessage());
        }
    }

    private void checkAgeSignals(final CallbackContext callbackContext) {
        try {
            ageSignalsManager.checkAgeSignals(AgeSignalsRequest.builder().build())
                    .addOnSuccessListener(ageSignalsResult -> {
                        try {
                            JSONObject resultObj = new JSONObject();

                            resultObj.put("ageRangeSource", mapAgeRangeSource(ageSignalsResult.ageRangeSource()));
                            resultObj.putOpt("ageLower", ageSignalsResult.ageLower());
                            resultObj.putOpt("ageUpper", ageSignalsResult.ageUpper());
                            resultObj.putOpt("installId", ageSignalsResult.installId());
                            resultObj.put("significantChangeStatus",
                                    mapSignificantChangeStatus(ageSignalsResult.significantChangeStatus()));

                            Date approvalDate = ageSignalsResult.significantChangeApprovalDate();
                            if (approvalDate != null) {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                                resultObj.put("significantChangeApprovalDate", sdf.format(approvalDate));
                            } else {
                                resultObj.put("significantChangeApprovalDate", JSONObject.NULL);
                            }

                            callbackContext.success(resultObj);
                        } catch (JSONException e) {
                            callbackContext.error("JSON_PARSING_ERROR: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> sendError(callbackContext, e));
        } catch (Exception e) {
            callbackContext.error("INITIALIZATION_ERROR: " + e.getMessage());
        }
    }

    private int parseAgeSignalsStatus(String status) {
        if ("NOT_SHARED".equalsIgnoreCase(status)) return AgeSignalsStatus.NOT_SHARED;
        if ("VERIFICATION_REQUIRED".equalsIgnoreCase(status)) return AgeSignalsStatus.VERIFICATION_REQUIRED;
        return AgeSignalsStatus.SHARED;
    }

    private Integer parseAgeRangeSource(String source) {
        if ("TIER_A".equalsIgnoreCase(source)) return AgeRangeSource.TIER_A;
        if ("TIER_B".equalsIgnoreCase(source)) return AgeRangeSource.TIER_B;
        if ("TIER_C".equalsIgnoreCase(source)) return AgeRangeSource.TIER_C;
        if ("TIER_D".equalsIgnoreCase(source)) return AgeRangeSource.TIER_D;
        return null;
    }

    private Integer parseSignificantChangeStatus(String status) {
        if ("APPROVED".equalsIgnoreCase(status)) return SignificantChangeStatus.APPROVED;
        if ("PENDING".equalsIgnoreCase(status)) return SignificantChangeStatus.PENDING;
        if ("DECLINED".equalsIgnoreCase(status)) return SignificantChangeStatus.DECLINED;
        return null;
    }

    private Object mapAgeSignalsStatus(Integer status) {
        if (status == null) return JSONObject.NULL;
        if (status == AgeSignalsStatus.SHARED) return "SHARED";
        if (status == AgeSignalsStatus.NOT_SHARED) return "NOT_SHARED";
        if (status == AgeSignalsStatus.VERIFICATION_REQUIRED) return "VERIFICATION_REQUIRED";
        return String.valueOf(status);
    }

    private Object mapAgeRangeSource(Integer source) {
        if (source == null) return JSONObject.NULL;
        if (source == AgeRangeSource.TIER_A) return "TIER_A";
        if (source == AgeRangeSource.TIER_B) return "TIER_B";
        if (source == AgeRangeSource.TIER_C) return "TIER_C";
        if (source == AgeRangeSource.TIER_D) return "TIER_D";
        return String.valueOf(source);
    }

    private Object mapSignificantChangeStatus(Integer status) {
        if (status == null) return JSONObject.NULL;
        if (status == SignificantChangeStatus.APPROVED) return "APPROVED";
        if (status == SignificantChangeStatus.PENDING) return "PENDING";
        if (status == SignificantChangeStatus.DECLINED) return "DECLINED";
        return String.valueOf(status);
    }

    private void sendError(CallbackContext callbackContext, Exception e) {
        try {
            JSONObject errorObj = new JSONObject();
            if (e instanceof AgeSignalsException) {
                AgeSignalsException ase = (AgeSignalsException) e;
                errorObj.put("code", ase.getErrorCode());
                errorObj.put("message", ase.getMessage());
            } else {
                errorObj.put("code", -100);
                errorObj.put("message", e.getMessage());
            }
            callbackContext.error(errorObj);
        } catch (JSONException je) {
            callbackContext.error("API_REQUEST_FAILED: " + e.getMessage());
        }
    }
}
