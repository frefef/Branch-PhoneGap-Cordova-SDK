package io.branch.referral;

import io.branch.referral.Branch.BranchLinkCreateListener;
import io.branch.referral.Branch.BranchReferralInitListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

public class CDVBranch extends CordovaPlugin {
	private Branch branch_;
	private Context context_;
    private Intent intent_;

     @Override
     public void onNewIntent(Intent intent) {
        this.intent_ = intent;
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
        this.context_ = ((CordovaActivity) this.webView.getContext()).getApplicationContext();
        if (this.intent_ == null) {
            this.intent_ = ((CordovaActivity) this.webView.getContext()).getIntent();
        }

    	if (action.equals("getInstance")) {
    		if (args.length() > 0) {
    			this.branch_ = Branch.getInstance(context_, args.getString(0));
    		} else {
    			this.branch_ = Branch.getInstance(context_);
    		}
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    	} else if (action.equals("initSession")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance(context_);
    		}
    		final JSONObject retParams = new JSONObject();
    		if (args.length() > 0) {
    			this.branch_.initSession(new BranchReferralInitListener() {
					@Override
					public void onInitFinished(JSONObject referringParams) {
						intent_.setData(null);
                        try {
							retParams.put("data", referringParams);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
					}
    			}, args.getBoolean(0), intent_.getData());
    		} else {
    			this.branch_.initSession(new BranchReferralInitListener() {
					@Override
					public void onInitFinished(JSONObject referringParams) {
                        intent_.setData(null);
						try {
							retParams.put("data", referringParams);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
					}
    			}, intent_.getData());
    		}
    	} else if (action.equals("getFirstReferringParams")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance(context_);
    		}
    		final JSONObject retParams = new JSONObject();
    		retParams.put("data", this.branch_.getFirstReferringParams());
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
    	} else if (action.equals("getLatestReferringParams")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance(context_);
    		}
    		final JSONObject retParams = new JSONObject();
    		retParams.put("data", this.branch_.getLatestReferringParams());
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, retParams));
    	} else if (action.equals("closeSession")) {
            if (this.branch_ == null) {
                this.branch_ = Branch.getInstance(context_);
            }
            this.branch_.closeSession();
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
        } else if (action.equals("setIdentity")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance(context_);
    		}
    		final JSONObject retParams = new JSONObject();
    		if (args.length() > 0) {
    			this.branch_.setIdentity(args.getString(0), new BranchReferralInitListener() {
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
    	} else if (action.equals("logout")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance(context_);
    		}
    		this.branch_.logout();
    		callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
    	} else if (action.equals("userCompletedAction")) {
    		if (this.branch_ == null) {
    			this.branch_ = Branch.getInstance(context_);
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
    			this.branch_ = Branch.getInstance(context_);
    		}
    		final JSONObject retParams = new JSONObject();
    		String channel = "";
            String feature = "";
            int currentStringParam = 0;
    		JSONObject metadata = new JSONObject();
    		for (int i = 0; i < args.length(); i++) {
    			if (args.get(i) instanceof String) {
                    if (currentStringParam == 0) {
                        currentStringParam = 1;
                        channel = args.getString(i);
                    } else {
                        feature = args.getString(i);
                    }
    			} else if (args.get(i) instanceof JSONObject) {
    				metadata = args.getJSONObject(i);
    			}
    		}
    		this.branch_.getShortUrl(channel, feature, null, metadata, new BranchLinkCreateListener() {
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
    	} else if (action.equals("getContentUrl")) {
            if (this.branch_ == null) {
                this.branch_ = Branch.getInstance(context_);
            }
            final JSONObject retParams = new JSONObject();
            String channel = "";
            JSONObject metadata = new JSONObject();
            for (int i = 0; i < args.length(); i++) {
                if (args.get(i) instanceof String) {
                    channel = args.getString(i);
                } else if (args.get(i) instanceof JSONObject) {
                    metadata = args.getJSONObject(i);
                }
            }
            this.branch_.getContentUrl(channel, metadata, new BranchLinkCreateListener() {
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