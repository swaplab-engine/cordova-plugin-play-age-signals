package cordova.plugin.playagesignals;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.Log;

import com.google.android.play.agesignals.AgeSignalsManager;
import com.google.android.play.agesignals.AgeSignalsManagerFactory;
import com.google.android.play.agesignals.AgeSignalsRequest;

public class PlayAgeSignalsPlugin extends CordovaPlugin {

    private static final String TAG = "PlayAgeSignalsPlugin";
    private static final String ACTION_CHECK_AGE_SIGNALS = "checkAgeSignals";

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (ACTION_CHECK_AGE_SIGNALS.equals(action)) {
            this.cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    checkAgeSignals(callbackContext);
                }
            });
            return true;
        }

        return false;
    }

    private void checkAgeSignals(final CallbackContext callbackContext) {
        Context context = this.cordova.getActivity().getApplicationContext();

        try {
            AgeSignalsManager ageSignalsManager = AgeSignalsManagerFactory.create(context);

            ageSignalsManager.checkAgeSignals(AgeSignalsRequest.builder().build())
                    .addOnSuccessListener(ageSignalsResult -> {

                        try {
                            JSONObject resultObj = new JSONObject();

                            if (ageSignalsResult.userStatus() != null) {
                                resultObj.put("userStatus", ageSignalsResult.userStatus().toString());
                            } else {
                                resultObj.put("userStatus", JSONObject.NULL);
                            }

                            resultObj.putOpt("ageLower", ageSignalsResult.ageLower());
                            resultObj.putOpt("ageUpper", ageSignalsResult.ageUpper());
                            resultObj.putOpt("installId", ageSignalsResult.installId());

                            if (ageSignalsResult.mostRecentApprovalDate() != null) {
                                resultObj.put("mostRecentApprovalDate", ageSignalsResult.mostRecentApprovalDate().toString());
                            } else {
                                resultObj.put("mostRecentApprovalDate", JSONObject.NULL);
                            }
                            callbackContext.success(resultObj);

                        } catch (JSONException e) {
                            callbackContext.error("JSON_PARSING_ERROR: " + e.getMessage());
                        }
                    })
                    .addOnFailureListener(e -> {
                        callbackContext.error("API_REQUEST_FAILED: " + e.getMessage());
                    });

        } catch (Exception e) {
            callbackContext.error("INITIALIZATION_ERROR: " + e.getMessage());
        }
    }
}
