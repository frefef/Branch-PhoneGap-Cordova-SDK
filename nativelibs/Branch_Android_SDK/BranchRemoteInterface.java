package io.branch.referral;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.util.DisplayMetrics;

public class BranchRemoteInterface extends RemoteInterface {
	public static final String REQ_TAG_REGISTER_INSTALL = "t_register_install";
	public static final String REQ_TAG_REGISTER_OPEN = "t_register_open";
	public static final String REQ_TAG_REGISTER_CLOSE = "t_register_close";
	public static final String REQ_TAG_COMPLETE_ACTION = "t_complete_action";
	public static final String REQ_TAG_GET_REFERRAL_COUNTS = "t_get_referral_counts";
	public static final String REQ_TAG_GET_REWARDS = "t_get_rewards";
	public static final String REQ_TAG_REDEEM_REWARDS = "t_redeem_rewards";
	public static final String REQ_TAG_GET_REWARD_HISTORY = "t_get_reward_history";
	public static final String REQ_TAG_GET_CUSTOM_URL = "t_get_custom_url";
	public static final String REQ_TAG_IDENTIFY = "t_identify_user";
	public static final String REQ_TAG_LOGOUT = "t_logout";
	public static final String REQ_TAG_GET_REFERRAL_CODE = "t_get_referral_code";
	public static final String REQ_TAG_VALIDATE_REFERRAL_CODE = "t_validate_referral_code";

	private SystemObserver sysObserver_;
	private PrefHelper prefHelper_;
	private NetworkCallback callback_;
	
	public BranchRemoteInterface() {}
	
	public BranchRemoteInterface(Context context) {
		prefHelper_ = PrefHelper.getInstance(context);
		sysObserver_ = new SystemObserver(context);
	}
	
	public void setNetworkCallbackListener(NetworkCallback callback) {
		callback_ = callback;
	}
	
	public void registerInstall(String installID, boolean debug) {
		String urlExtend = "v1/install";
		if (callback_ != null) {
			JSONObject installPost = new JSONObject();
			try {
				installPost.put("app_id", prefHelper_.getAppKey());
				if (!installID.equals(PrefHelper.NO_STRING_VALUE))
					installPost.put("link_click_id", installID);
				if (!sysObserver_.getUniqueID().equals(SystemObserver.BLANK)) {
					installPost.put("hardware_id", sysObserver_.getUniqueID());
					installPost.put("is_hardware_id_real", sysObserver_.hasRealHardwareId());
				}
				if (!sysObserver_.getAppVersion().equals(SystemObserver.BLANK))
					installPost.put("app_version", sysObserver_.getAppVersion());
				if (!sysObserver_.getCarrier().equals(SystemObserver.BLANK))
					installPost.put("carrier", sysObserver_.getCarrier());
				installPost.put("bluetooth", sysObserver_.getBluetoothPresent());
				if (!sysObserver_.getBluetoothVersion().equals(SystemObserver.BLANK))
					installPost.put("bluetooth_version", sysObserver_.getBluetoothVersion());
				installPost.put("has_nfc", sysObserver_.getNFCPresent());
				installPost.put("has_telephone", sysObserver_.getTelephonePresent());
				if (!sysObserver_.getPhoneBrand().equals(SystemObserver.BLANK))
					installPost.put("brand", sysObserver_.getPhoneBrand());
				if (!sysObserver_.getPhoneModel().equals(SystemObserver.BLANK))
					installPost.put("model", sysObserver_.getPhoneModel());
				if (!sysObserver_.getOS().equals(SystemObserver.BLANK))
					installPost.put("os", sysObserver_.getOS());
				String uriScheme = sysObserver_.getURIScheme();
				if (!uriScheme.equals(SystemObserver.BLANK)) 
					installPost.put("uri_scheme", uriScheme);
				installPost.put("os_version", sysObserver_.getOSVersion());
				DisplayMetrics dMetrics = sysObserver_.getScreenDisplay();
				installPost.put("screen_dpi", dMetrics.densityDpi);
				installPost.put("screen_height", dMetrics.heightPixels);
				installPost.put("screen_width", dMetrics.widthPixels);
				installPost.put("wifi", sysObserver_.getWifiConnected());
				installPost.put("is_referrable", prefHelper_.getIsReferrable());
				installPost.put("update", sysObserver_.getUpdateState());
				if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
					installPost.put("link_identifier", prefHelper_.getLinkClickIdentifier());
				}
				installPost.put("debug", debug);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(installPost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_INSTALL));
		}
	}
	
	public void registerOpen(boolean debug) {
		String urlExtend = "v1/open";
		if (callback_ != null) {
			JSONObject openPost = new JSONObject();
			try {
				openPost.put("app_id", prefHelper_.getAppKey());
				openPost.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
				openPost.put("identity_id", prefHelper_.getIdentityID());
				openPost.put("is_referrable", prefHelper_.getIsReferrable());
				if (!sysObserver_.getAppVersion().equals(SystemObserver.BLANK))
					openPost.put("app_version", sysObserver_.getAppVersion());
				openPost.put("os_version", sysObserver_.getOSVersion());
				String uriScheme = sysObserver_.getURIScheme();
				if (!uriScheme.equals(SystemObserver.BLANK)) 
					openPost.put("uri_scheme", uriScheme);
				if (!sysObserver_.getOS().equals(SystemObserver.BLANK))
					openPost.put("os", sysObserver_.getOS());
				if (!prefHelper_.getLinkClickIdentifier().equals(PrefHelper.NO_STRING_VALUE)) {
					openPost.put("link_identifier", prefHelper_.getLinkClickIdentifier());
				}
				openPost.put("debug", debug);
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(openPost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_OPEN));
		}
	}
	
	public void registerClose() {
		String urlExtend = "v1/close";
		if (callback_ != null) {
			JSONObject closePost = new JSONObject();
			try {
				closePost.put("app_id", prefHelper_.getAppKey());
				closePost.put("session_id", prefHelper_.getSessionID());
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			callback_.finished(make_restful_post(closePost, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REGISTER_CLOSE));
		}
	}
	
	public void userCompletedAction(JSONObject post) {
		String urlExtend = "v1/event";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_COMPLETE_ACTION));
		}
	}
	
	public void redeemRewards(JSONObject post) {
		String urlExtend = "v1/redeem";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_REDEEM_REWARDS));
		}
	}
	
	public void getRewards() {
		String urlExtend = "v1/credits/" + prefHelper_.getIdentityID();
		if (callback_ != null) {
			callback_.finished(make_restful_get(prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REWARDS));
		}
	}
	
	public void getReferralCounts() {
		String urlExtend = "v1/referrals/" + prefHelper_.getIdentityID();
		if (callback_ != null) {
			callback_.finished(make_restful_get(prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REFERRAL_COUNTS));
		}
	}
	
	public void getCreditHistory(JSONObject post) {
		String params = this.convertJSONtoString(post);
		String urlExtend = "v1/credithistory" + params;
		if (callback_ != null) {
			callback_.finished(make_restful_get(prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REWARD_HISTORY));
		}
	}
	
	public void createCustomUrl(JSONObject post) {
		String urlExtend = "v1/url";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_CUSTOM_URL));
		}
	}
	
	public void identifyUser(JSONObject post) {
		String urlExtend = "v1/profile";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_IDENTIFY));
		}
	}
	
	public void logoutUser(JSONObject post) {
		String urlExtend = "v1/logout";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_LOGOUT));
		}
	}
	
	public void getReferralCode(JSONObject post) {
		String urlExtend = "v1/referralcode";
		if (callback_ != null) {
			callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_GET_REFERRAL_CODE));
		}
	}
	
	public void validateReferralCode(JSONObject post) {
		String urlExtend;
		try {
			urlExtend = "v1/referralcode/" + post.getString("referral_code");
			if (callback_ != null) {
				callback_.finished(make_restful_post(post, prefHelper_.getAPIBaseUrl() + urlExtend, REQ_TAG_VALIDATE_REFERRAL_CODE));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	private String convertJSONtoString(JSONObject json) {
		StringBuilder result = new StringBuilder();
		
		if (json != null) {
	        Iterator<String> iter = json.keys();
	        if (iter.hasNext()) {
	        	boolean first = true;
	        	do {
	        		if (first) {
		        		result.append("?");
		        		first = false;
		        	} else {
		        		result.append("&");
		        	}
	        		
	        		String key = iter.next();
	        		String value;
	        		try {
						value = json.getString(key);
					} catch (JSONException e) {
						e.printStackTrace();
						return null;
					}
	        		
	        		result.append(key + "=" + value);
	        	} while (iter.hasNext());
	        }
	    }
		
		return result.toString();
	}
}
