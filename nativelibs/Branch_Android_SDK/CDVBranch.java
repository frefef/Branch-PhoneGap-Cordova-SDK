package io.branch.referral;

import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

public class CDVBranch extends CordovaPlugin {
	private Branch branch_;
	private Context context_;
	
	@Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    	super.initialize(cordova, webView);
    	this.context_ = cordova.getActivity().getApplicationContext();
	}
	
	/**
     * Executes the request and returns whether the action was valid.
     *
     * @param action 		The action to execute.
     * @param args 		JSONArray of arguments for the plugin.
     * @param callbackContext	The callback context used when calling back into JavaScript.
     * @return 			True if the action was valid, false otherwise.
     */
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
    	if (action.equals("getInstance")) {
    		if (args.length() > 0) {
    			this.branch_ = Branch.getInstance(context_, args.getString(0));
    		} else {
    			this.branch_ = Branch.getInstance();
    		}
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    	} else if (action.equals("initUserSession")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance();
    		}
    		final JSONObject retParams = new JSONObject();
    		if (args.length() > 0) {
    			this.branch_.initUserSession(new BranchReferralInitListener() {
					@Override
					public void onInitFinished(JSONObject referringParams) {
						try {
							retParams.put("data", referringParams);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
					}
    			}, args.getBoolean(0));
    		} else {
    			this.branch_.initUserSession(new BranchReferralInitListener() {
					@Override
					public void onInitFinished(JSONObject referringParams) {
						try {
							retParams.put("data", referringParams);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
					}
    			});
    		}
    	} else if (action.equals("getInstallReferringParams")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance();
    		}
    		final JSONObject retParams = new JSONObject();
    		retParams.put("data", this.branch_.getInstallReferringParams());
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
    	} else if (action.equals("getReferringParams")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance();
    		}
    		final JSONObject retParams = new JSONObject();
    		retParams.put("data", this.branch_.getReferringParams());
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
    	} else if (action.equals("hasIdentity")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance();
    		}
    		final JSONObject retParams = new JSONObject();
    		retParams.put("check", this.branch_.hasIdentity());
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
    	} else if (action.equals("identifyUser")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance();
    		}
    		final JSONObject retParams = new JSONObject();
    		if (args.length() > 0) {
    			this.branch_.identifyUser(args.getString(0), new BranchReferralInitListener() {
					@Override
					public void onInitFinished(JSONObject referringParams) {
						try {
							retParams.put("data", referringParams);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
					}
    			});
    		} else {
    			callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, retParams));
    		}
    	} else if (action.equals("clearUser")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance();
    		}
    		this.branch_.clearUser();
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    	} else if (action.equals("userCompletedAction")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance();
    		}
    		String tag = "";
    		JSONObject metadata = new JSONObject();
    		for (int i = 0; i < args.length(); i++) {
    			if (args.get(i) instanceof String) {
    				tag = args.getString(i);
    			} else if (args.get(i) instanceof JSONObject) {
    				metadata = args.getJSONObject(i);
    			}
    		}
    		this.branch_.userCompletedAction(tag, metadata);
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    		
    	} else if (action.equals("getShortUrl")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance();
    		}
    		final JSONObject retParams = new JSONObject();
    		String tag = "";
    		JSONObject metadata = new JSONObject();
    		for (int i = 0; i < args.length(); i++) {
    			if (args.get(i) instanceof String) {
    				tag = args.getString(i);
    			} else if (args.get(i) instanceof JSONObject) {
    				metadata = args.getJSONObject(i);
    			}
    		}
    		this.branch_.getShortUrl(tag, metadata, new BranchLinkCreateListener() {
				@Override
				public void onLinkCreate(String url) {
					try {
						retParams.put("url", url);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
				}
    		});
    	} else {
    		return false;
    	}
    	return true;
    }
	
}