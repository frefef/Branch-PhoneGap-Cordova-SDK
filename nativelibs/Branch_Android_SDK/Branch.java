package io.branch.referral;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;

public class Branch {	
	public static String FEATURE_TAG_SHARE = "share";
	public static String FEATURE_TAG_REFERRAL = "referral";
	public static String FEATURE_TAG_INVITE = "invite";
	public static String FEATURE_TAG_DEAL = "deal";
	public static String FEATURE_TAG_GIFT = "gift";
	
	public static String REDEEM_CODE = "$redeem_code";
	public static String REEFERRAL_BUCKET_DEFAULT = "default";
	public static String REFERRAL_CODE_TYPE = "credit";
	public static int REFERRAL_CREATION_SOURCE_SDK = 2;
	
	public static int REFERRAL_CODE_LOCATION_REFERREE = 0;
	public static int REFERRAL_CODE_LOCATION_REFERRING_USER = 2;
	public static int REFERRAL_CODE_LOCATION_BOTH = 3;
	
	public static int REFERRAL_CODE_AWARD_UNLIMITED = 1;
	public static int REFERRAL_CODE_AWARD_UNIQUE = 0;
	
	private static final int SESSION_KEEPALIVE = 100;
	private static final int INTERVAL_RETRY = 3000;
	private static final int MAX_RETRIES = 5;

	private static Branch branchReferral_;
	private boolean isInit_;
	
	private BranchReferralInitListener initSessionFinishedCallback_;
	private BranchReferralInitListener initIdentityFinishedCallback_;
	private BranchReferralStateChangedListener stateChangedCallback_;
	private BranchLinkCreateListener linkCreateCallback_;
	private BranchListResponseListener creditHistoryCallback_;
	private BranchReferralInitListener getReferralCodeCallback_;
	private BranchReferralInitListener validateReferralCodeCallback_;
	
	private BranchRemoteInterface kRemoteInterface_;
	private PrefHelper prefHelper_;
	private SystemObserver systemObserver_;
	private Context context_;
	
	private Timer closeTimer;
	private boolean keepAlive_;
	
	private Semaphore serverSema_;
	private ServerRequestQueue requestQueue_;
	private int networkCount_;
	private int retryCount_;
	
	private boolean initFinished_;
	private boolean hasNetwork_;
	
	private boolean debug_;
	
	private Branch(Context context) {
		prefHelper_ = PrefHelper.getInstance(context);
		kRemoteInterface_ = new BranchRemoteInterface(context);
		systemObserver_ = new SystemObserver(context);
		kRemoteInterface_.setNetworkCallbackListener(new ReferralNetworkCallback());
		requestQueue_ = ServerRequestQueue.getInstance(context);
		serverSema_ = new Semaphore(1);
		closeTimer = new Timer();
		keepAlive_ = false;
		isInit_ = false;
		networkCount_ = 0;
		initFinished_ = false;
		hasNetwork_ = true;
		debug_ = false;
	}
	
	public static Branch getInstance(Context context, String key) {
		if (branchReferral_ == null) {
			branchReferral_ = Branch.initInstance(context);
		}
		branchReferral_.context_ = context;
		branchReferral_.prefHelper_.setAppKey(key);
		return branchReferral_;
	}
	
	public static Branch getInstance(Context context) {
		if (branchReferral_ == null) {
			branchReferral_ = Branch.initInstance(context);
		}
		branchReferral_.context_ = context;
		return branchReferral_;
	}
	
	private static Branch initInstance(Context context) {
		return new Branch(context.getApplicationContext());
	}
	
	public void resetUserSession() {
		isInit_ = false;
	}
	
	// if you want to flag debug, call this before initUserSession
	public void setDebug() {
		debug_ = true;
	}
	
	@Deprecated
	public void initUserSession(BranchReferralInitListener callback) {
		initSession(callback);
	}

	public void initSession(BranchReferralInitListener callback) {
		if (systemObserver_.getUpdateState() == 0 && !hasUser()) {
			prefHelper_.setIsReferrable();
		} else {
			prefHelper_.clearIsReferrable();
		}
		initUserSessionInternal(callback);
	}

	@Deprecated
	public void initUserSession(BranchReferralInitListener callback, Uri data) {
		initSession(callback, data);
	}

	public void initSession(BranchReferralInitListener callback, Uri data) {
		if (data != null) {
			if (data.getQueryParameter("link_click_id") != null) {
				prefHelper_.setLinkClickIdentifier(data.getQueryParameter("link_click_id"));
			}
		}
		initSession(callback);
	}

	@Deprecated
	public void initUserSession() {
		initSession();
	}

	public void initSession() {
		initSession(null);
	}

	@Deprecated
	public void initUserSessionWithData(Uri data) {
		initSessionWithData(data);
	}

	public void initSessionWithData(Uri data) {
		if (data != null) {
			if (data.getQueryParameter("link_click_id") != null) {
				prefHelper_.setLinkClickIdentifier(data.getQueryParameter("link_click_id"));
			}
		}
		initSession(null);
	}

	@Deprecated
	public void initUserSession(boolean isReferrable) {
		initSession(isReferrable);
	}

	public void initSession(boolean isReferrable) {
		initSession(null, isReferrable);
	}

	@Deprecated
	public void initUserSession(BranchReferralInitListener callback,
			boolean isReferrable, Uri data) {
		initSession(callback, isReferrable, data);
	}

	public void initSession(BranchReferralInitListener callback,
			boolean isReferrable, Uri data) {
		if (data != null) {
			if (data.getQueryParameter("link_click_id") != null) {
				prefHelper_.setLinkClickIdentifier(data.getQueryParameter("link_click_id"));
			}
		}
		initSession(callback, isReferrable);
	}

	@Deprecated
	public void initUserSession(BranchReferralInitListener callback,
			boolean isReferrable) {
		initSession(callback, isReferrable);
	}

	public void initSession(BranchReferralInitListener callback,
			boolean isReferrable) {
		if (isReferrable) {
			this.prefHelper_.setIsReferrable();
		} else {
			this.prefHelper_.clearIsReferrable();
		}
		initUserSessionInternal(callback);
	}
	
	private void initUserSessionInternal(BranchReferralInitListener callback) {
		initSessionFinishedCallback_ = callback;
		
		if (!isInit_) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					initializeSession();
				}
			}).start();
			isInit_ = true;
		} else {
			boolean installOrOpenInQueue = requestQueue_.containsInstallOrOpen();
			if (hasUser() && hasSession() && !installOrOpenInQueue) {
				if (callback != null) callback.onInitFinished(new JSONObject());
			} else {
				if (!installOrOpenInQueue) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							initializeSession();
						}
					}).start();
				} else {
					processNextQueueItem();
				}
			}
		}
	}
	
	public void closeSession() {
		if (keepAlive_) {
			return;
		}
		
		// else, real close
		isInit_ = false;
		
		if (!hasNetwork_) {
	        // if there's no network connectivity, purge the old install/open
	        ServerRequest req = requestQueue_.peek();
	        if (req != null && (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN))) {
	            requestQueue_.dequeue();
	        }
	    } else {
	    	new Thread(new Runnable() {
				@Override
				public void run() {
					requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE, null));
					
					if (initFinished_ || !hasNetwork_) {
						processNextQueueItem();
					}
				}
			}).start();
	    }
	}
	
	@Deprecated
	public void identifyUser(String userId, BranchReferralInitListener callback) {
		setIdentity(userId, callback);
	}
	
	public void setIdentity(String userId, BranchReferralInitListener callback) {
		initIdentityFinishedCallback_ = callback;
		setIdentity(userId);
	}

	@Deprecated
	public void identifyUser(final String userId) {
		setIdentity(userId);
	}
	
	public void setIdentity(final String userId) {
		if (userId == null || userId.length() == 0) {
			return;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("identity", userId);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_IDENTIFY, post));

				if (initFinished_ || !hasNetwork_) {
					processNextQueueItem();
				}
			}
		}).start();
	}

	@Deprecated
	public void clearUser() {
		logout();
	}
	
	public void logout() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("session_id", prefHelper_.getSessionID());
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_LOGOUT, post));

				if (initFinished_ || !hasNetwork_) {
					processNextQueueItem();
				}
			}
		}).start();
	}
	
	public void loadActionCounts() {
		loadActionCounts(null);
	}
	
	public void loadActionCounts(BranchReferralStateChangedListener callback) {
		stateChangedCallback_ = callback;
		new Thread(new Runnable() {
			@Override
			public void run() {
				requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS, null));
				
				if (initFinished_ || !hasNetwork_) {
					processNextQueueItem();
				}
			}
		}).start();
	}
	
	public void loadRewards() {
		loadRewards(null);
	}
	
	public void loadRewards(BranchReferralStateChangedListener callback) {
		stateChangedCallback_ = callback;
		new Thread(new Runnable() {
			@Override
			public void run() {
				requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REWARDS, null));
				
				if (initFinished_ || !hasNetwork_) {
					processNextQueueItem();
				}
			}
		}).start();
	}
	
	public int getCredits() {
		return prefHelper_.getCreditCount();
	}
	
	public int getCreditsForBucket(String bucket) {
		return prefHelper_.getCreditCount(bucket);
	}
	
	public int getTotalCountsForAction(String action) {
		return prefHelper_.getActionTotalCount(action);
	}
	
	public int getUniqueCountsForAction(String action) {
		return prefHelper_.getActionUniqueCount(action);
	}
	
	public void redeemRewards(int count) {
		redeemRewards("default", count);
	}
	
	public void redeemRewards(final String bucket, final int count) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				int creditsToRedeem = 0;
				int credits = prefHelper_.getCreditCount(bucket);
				
				if (count > credits) {
					creditsToRedeem = credits;
					Log.i("BranchSDK", "Branch Warning: You're trying to redeem more credits than are available. Have you updated loaded rewards");
				} else {
					creditsToRedeem = count;
				}
				
				if (creditsToRedeem > 0) {
					retryCount_ = 0;
					JSONObject post = new JSONObject();
					try {
						post.put("app_id", prefHelper_.getAppKey());
						post.put("identity_id", prefHelper_.getIdentityID());
						post.put("bucket", bucket);
						post.put("amount", creditsToRedeem);
					} catch (JSONException ex) {
						ex.printStackTrace();
						return;
					}
					requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_REDEEM_REWARDS, post));
					
					if (initFinished_ || !hasNetwork_) {
						processNextQueueItem();
					}
				}
			}
		}).start();
	}
	
	public void getCreditHistory(BranchListResponseListener callback) {
		getCreditHistory(null, null, 100, CreditHistoryOrder.kMostRecentFirst, callback);
	}
	
	public void getCreditHistory(final String bucket, BranchListResponseListener callback) {
		getCreditHistory(bucket, null, 100, CreditHistoryOrder.kMostRecentFirst, callback);
	}
	
	public void getCreditHistory(final String afterId, final int length, final CreditHistoryOrder order, BranchListResponseListener callback) {
		getCreditHistory(null, afterId, length, order, callback);
	}
	
	public void getCreditHistory(final String bucket, final String afterId, final int length, final CreditHistoryOrder order, BranchListResponseListener callback) {
		creditHistoryCallback_ = callback;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("length", length);
					post.put("direction", order.ordinal());

					if (bucket != null) {
						post.put("bucket", bucket);
					}
					
					if (afterId != null) {
						post.put("begin_after_id", afterId);
					}
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REWARD_HISTORY, post));
				
				if (initFinished_ || !hasNetwork_) {
					processNextQueueItem();
				}
			}
		}).start();
	}
	
	public void userCompletedAction(final String action, final JSONObject metadata) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				retryCount_ = 0;
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("session_id", prefHelper_.getSessionID());
					post.put("event", action);
					if (metadata != null) post.put("metadata", metadata);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION, post));
				
				if (initFinished_ || !hasNetwork_) {
					processNextQueueItem();
				}
			}
		}).start();
	}
	
	public void userCompletedAction(final String action) {
		userCompletedAction(action, null);
	}
	
	@Deprecated
	public JSONObject getInstallReferringParams() {
		return getFirstReferringParams();
	}
	
	public JSONObject getFirstReferringParams() {
		String storedParam = prefHelper_.getInstallParams();
		return convertParamsStringToDictionary(storedParam);
	}

	@Deprecated
	public JSONObject getReferringParams() {
		return getLatestReferringParams();
	}
	
	public JSONObject getLatestReferringParams() {
		String storedParam = prefHelper_.getSessionParams();
		return convertParamsStringToDictionary(storedParam);
	}
	
	public void getShortUrl(BranchLinkCreateListener callback) {
		generateShortLink(null, null, null, null, stringifyParams(null), callback);
	}
	
	public void getShortUrl(JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, null, null, null, stringifyParams(params), callback);
	}
	
	public void getReferralUrl(String channel, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(params), callback);
	}
	
	public void getReferralUrl(Collection<String> tags, String channel, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(tags, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(params), callback);
	}
	
	public void getContentUrl(String channel, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, channel, FEATURE_TAG_SHARE, null, stringifyParams(params), callback);
	}
	
	public void getContentUrl(Collection<String> tags, String channel, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(tags, channel, FEATURE_TAG_SHARE, null, stringifyParams(params), callback);
	}
	
	public void getShortUrl(String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, channel, feature, stage, stringifyParams(params), callback);
	}
	
	public void getShortUrl(Collection<String> tags, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(tags, channel, feature, stage, stringifyParams(params), callback);
	}
	

	public void getReferralCode(final int amount, BranchReferralInitListener callback) {
		this.getReferralCode(null, amount, null, REEFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
	}
	
	public void getReferralCode(final String prefix, final int amount, BranchReferralInitListener callback) {
		this.getReferralCode(prefix, amount, null, REEFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
	}
	
	public void getReferralCode(final int amount, final Date expiration, BranchReferralInitListener callback) {
		this.getReferralCode(null, amount, expiration, REEFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
	}
	
	public void getReferralCode(final String prefix, final int amount, final Date expiration, BranchReferralInitListener callback) {
		this.getReferralCode(prefix, amount, expiration, REEFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
	}
	
	public void getReferralCode(final String prefix, final int amount, final int calculationType, final int location, BranchReferralInitListener callback) {
		this.getReferralCode(prefix, amount, null, REEFERRAL_BUCKET_DEFAULT, calculationType, location, callback);
	}
	
	public void getReferralCode(final String prefix, final int amount, final Date expiration, final String bucket, final int calculationType, final int location, BranchReferralInitListener callback) {
		getReferralCodeCallback_ = callback;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("calculation_type", calculationType);
					post.put("location", location);
					post.put("type", REFERRAL_CODE_TYPE);
					post.put("creation_source", REFERRAL_CREATION_SOURCE_SDK);
					post.put("amount", amount);
					post.put("bucket", bucket != null ? bucket : REEFERRAL_BUCKET_DEFAULT);
					if (prefix != null && prefix.length() > 0) {
						post.put("prefix", prefix);
					}
					if (expiration != null) {
						post.put("expiration", convertDate(expiration));
					}
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_CODE, post));
				
				if (initFinished_ || !hasNetwork_) {
					processNextQueueItem();
				}
			}
		}).start();
	}
	
	public void validateReferralCode(final String code, BranchReferralInitListener callback) {
		validateReferralCodeCallback_ = callback;
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("referral_code", code);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_VALIDATE_REFERRAL_CODE, post));
				
				if (initFinished_ || !hasNetwork_) {
					processNextQueueItem();
				}
			}
		}).start();
	}
	
	public void applyReferralCode(final String code, final BranchReferralInitListener callback) {
		this.validateReferralCode(code, new BranchReferralInitListener() {
			@Override
			public void onInitFinished(JSONObject referringParams) {
				if (referringParams.has("referral_code")) {
					userCompletedAction(REDEEM_CODE + "-" + code);
					if (callback != null) {
						callback.onInitFinished(referringParams);
					}
				} else {
					if (callback != null) {
						callback.onInitFinished(new JSONObject());
					}
				}
			}
		});
		
	}
	
	// PRIVATE FUNCTIONS
	
	private String convertDate(Date date) {
		return android.text.format.DateFormat.format("yyyy-MM-dd", date).toString();
	}
	
	private String stringifyParams(JSONObject params) {
		if (params == null) {
			params = new JSONObject();
		}
		
		try {
			params.put("source", "android");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return params.toString();
	}

	private void generateShortLink(final Collection<String> tags, final String channel, final String feature, final String stage, final String params, BranchLinkCreateListener callback) {
		linkCreateCallback_ = callback;
		if (hasUser()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					JSONObject linkPost = new JSONObject();
					try {
						linkPost.put("app_id", prefHelper_.getAppKey());
						linkPost.put("identity_id", prefHelper_.getIdentityID());
						
						if (tags != null) {
							JSONArray tagArray = new JSONArray();
							for (String tag : tags) 
								tagArray.put(tag);
							linkPost.put("tags", tagArray);
						}
						if (channel != null) {
							linkPost.put("channel", channel);
						}
						if (feature != null) {
							linkPost.put("feature", feature);
						}
						if (stage != null) {
							linkPost.put("stage", stage);
						}
						if (params != null)
							linkPost.put("data", params);
					} catch (JSONException ex) {
						ex.printStackTrace();
					}
					requestQueue_.enqueue(new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL, linkPost));
					
					if (initFinished_ || !hasNetwork_) {
						processNextQueueItem();
					}
				}
			}).start();
		}
	}
	
	private JSONObject convertParamsStringToDictionary(String paramString) {
		if (paramString.equals(PrefHelper.NO_STRING_VALUE)) {
			return new JSONObject();
		} else {
			try {
				return new JSONObject(paramString);
			} catch (JSONException e) {
				byte[] encodedArray = Base64.decode(paramString.getBytes(), Base64.NO_WRAP);
				try {
					return new JSONObject(new String(encodedArray));
				} catch (JSONException ex) {
					ex.printStackTrace();
					return new JSONObject();
				}
			}
		}
	}
	
	private void processNextQueueItem() {
		try {
			serverSema_.acquire();
			if (networkCount_ == 0 && requestQueue_.getSize() > 0) {
				networkCount_ = 1;
				serverSema_.release();
				
				ServerRequest req = requestQueue_.peek();
				
				if (!req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE)) {
					keepAlive();
				}
				
				if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
					kRemoteInterface_.registerInstall(PrefHelper.NO_STRING_VALUE, debug_);
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
					kRemoteInterface_.registerOpen(debug_);
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS) && hasUser() && hasSession()) {
					kRemoteInterface_.getReferralCounts();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARDS) && hasUser() && hasSession()) {
					kRemoteInterface_.getRewards();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REDEEM_REWARDS) && hasUser() && hasSession()) {
					kRemoteInterface_.redeemRewards(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARD_HISTORY) && hasUser() && hasSession()) {
					kRemoteInterface_.getCreditHistory(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION) && hasUser() && hasSession()){
					kRemoteInterface_.userCompletedAction(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL) && hasUser() && hasSession()) {
					kRemoteInterface_.createCustomUrl(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_IDENTIFY) && hasUser() && hasSession()) {
					kRemoteInterface_.identifyUser(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE) && hasUser() && hasSession()) {
					kRemoteInterface_.registerClose();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_LOGOUT) && hasUser() && hasSession()) {
					kRemoteInterface_.logoutUser(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_CODE) && hasUser() && hasSession()) {
					kRemoteInterface_.getReferralCode(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_VALIDATE_REFERRAL_CODE) && hasUser() && hasSession()) {
					kRemoteInterface_.validateReferralCode(req.getPost());
				} else if (!hasUser()) {
					if (!hasAppKey() && hasSession()) {
						Log.i("BranchSDK", "Branch Warning: User session has not been initialized");
					} else {
						networkCount_ = 0;
						initSession();
					}
				}
			} else {
				serverSema_.release();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void handleFailure() {
		final ServerRequest req = requestQueue_.peek();
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN) ) {
					if (initSessionFinishedCallback_ != null) {
						JSONObject obj = new JSONObject();
						try {
							obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
						} catch(JSONException ex) {
							ex.printStackTrace();
						}
						initSessionFinishedCallback_.onInitFinished(obj);
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARDS)) {
					if (stateChangedCallback_ != null) {
						stateChangedCallback_.onStateChanged(false);
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARD_HISTORY)) {
					if (creditHistoryCallback_ != null) {
						creditHistoryCallback_.onReceivingResponse(null);
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
					if (linkCreateCallback_ != null) {
						linkCreateCallback_.onLinkCreate("Trouble reaching server. Please try again in a few minutes");
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_IDENTIFY)) {
					if (initIdentityFinishedCallback_ != null) {
						JSONObject obj = new JSONObject();
						try {
							obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
						} catch(JSONException ex) {
							ex.printStackTrace();
						}
						initIdentityFinishedCallback_.onInitFinished(obj);
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_CODE)) {
					if (getReferralCodeCallback_ != null) {
						getReferralCodeCallback_.onInitFinished(null);
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_VALIDATE_REFERRAL_CODE)) {
					if (validateReferralCodeCallback_ != null) {
						validateReferralCodeCallback_.onInitFinished(null);
					}
				}
			}
		});
	}
	
	private void retryLastRequest() {
		retryCount_ = retryCount_ + 1;
		if (retryCount_ > MAX_RETRIES) {
			handleFailure();
			requestQueue_.dequeue();
			retryCount_ = 0;
		} else {
			try {
				Thread.sleep(INTERVAL_RETRY);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void updateAllRequestsInQueue() {
		try {
			for (int i = 0; i < requestQueue_.getSize(); i++) {
				ServerRequest req = requestQueue_.peekAt(i);
				if (req.getPost() != null) {
					Iterator<?> keys = req.getPost().keys();
		    		while (keys.hasNext()) {
		    			String key = (String)keys.next();
		    			if (key.equals("app_id")) {
		    				req.getPost().put(key, prefHelper_.getAppKey());
		    			} else if (key.equals("session_id")) {
		    				req.getPost().put(key, prefHelper_.getSessionID());
		    			} else if (key.equals("identity_id")) {
		    				req.getPost().put(key, prefHelper_.getIdentityID());
		    			}
		    		}	
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}	
	}
	
	private void clearTimer() {
		if (closeTimer == null)
			return;
		closeTimer.cancel();
		closeTimer.purge();
		closeTimer = new Timer();
	}
	
	private void keepAlive() {
		keepAlive_ = true;
		clearTimer();
		closeTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				new Thread(new Runnable() {
					@Override
					public void run() {
						keepAlive_ = false;
					}
				}).start();
			}
		}, SESSION_KEEPALIVE);
	}
	
	private boolean hasAppKey() {
		return !prefHelper_.getAppKey().equals(PrefHelper.NO_STRING_VALUE);
	}
	
	private boolean hasSession() {
		return !prefHelper_.getSessionID().equals(PrefHelper.NO_STRING_VALUE);
	}
	
	private boolean hasUser() {
		return !prefHelper_.getIdentityID().equals(PrefHelper.NO_STRING_VALUE);
	}
	
	private void insertRequestAtFront(ServerRequest req) {
		if (networkCount_ == 0) {
			requestQueue_.insert(req, 0);
		} else {
			requestQueue_.insert(req, 1);
		}
	}
	
	private void registerInstallOrOpen(String tag) {
		if (!requestQueue_.containsInstallOrOpen()) {
			insertRequestAtFront(new ServerRequest(tag));
		} else {
			requestQueue_.moveInstallOrOpenToFront(tag, networkCount_);
		}
		processNextQueueItem();
	}
	
	private void initializeSession() {
		if (hasUser()) {
			registerInstallOrOpen(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN);
		} else {
			registerInstallOrOpen(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL);
		}
	} 
	
	private void processReferralCounts(ServerResponse resp) {
		boolean updateListener = false;
		Iterator<?> keys = resp.getObject().keys();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			
			try {
				JSONObject counts = resp.getObject().getJSONObject(key);
				int total = counts.getInt("total");
				int unique = counts.getInt("unique");
				
				if (total != prefHelper_.getActionTotalCount(key) || unique != prefHelper_.getActionUniqueCount(key)) {
					updateListener = true;
				}
				prefHelper_.setActionTotalCount(key, total);
				prefHelper_.setActionUniqueCount(key, unique);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
		final boolean finUpdateListener = updateListener;
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (stateChangedCallback_ != null) {
					stateChangedCallback_.onStateChanged(finUpdateListener);
				}
			}
		});
	}
	
	private void processRewardCounts(ServerResponse resp) {
		boolean updateListener = false;
		Iterator<?> keys = resp.getObject().keys();
		while (keys.hasNext()) {
			String key = (String)keys.next();
			
			try {
				int credits = resp.getObject().getInt(key);
				
				if (credits != prefHelper_.getCreditCount(key)) {
					updateListener = true;
				}
				prefHelper_.setCreditCount(key, credits);
			} catch (JSONException e) {
				e.printStackTrace();
			}			
		}
		final boolean finUpdateListener = updateListener;
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (stateChangedCallback_ != null) {
					stateChangedCallback_.onStateChanged(finUpdateListener);
				}
			}
		});
	}
	
	private void processCreditHistory(final ServerResponse resp) {
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (creditHistoryCallback_ != null) {
					creditHistoryCallback_.onReceivingResponse(resp.getArray());
				}
			}
		});
	}
	
	private void processReferralCodeGet(final ServerResponse resp) {
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (getReferralCodeCallback_ != null) {
					try {
						JSONObject data = resp.getObject();
						String event = data.getString("event");
						String code = event.substring(REDEEM_CODE.length() + 1);
				        data.put("referral_code", code);
				        getReferralCodeCallback_.onInitFinished(data);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	private void processReferralCodeValidation(final ServerResponse resp) {
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (validateReferralCodeCallback_ != null) {
					try {
						JSONObject data = resp.getObject();
						String event = data.getString("event");
						String code = event.substring(REDEEM_CODE.length() + 1);
				        data.put("referral_code", code);
				        validateReferralCodeCallback_.onInitFinished(data);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
	
	public class ReferralNetworkCallback implements NetworkCallback {
		@Override
		public void finished(ServerResponse serverResponse) {
			if (serverResponse != null) {
				try {
					int status = serverResponse.getStatusCode();
					String requestTag = serverResponse.getTag();
					
					hasNetwork_ = true;
					
					if (status >= 400 && status < 500) {
						if (serverResponse.getObject().has("error") && serverResponse.getObject().getJSONObject("error").has("message")) {
							Log.i("BranchSDK", "Branch API Error: " + serverResponse.getObject().getJSONObject("error").getString("message"));
						}
						requestQueue_.dequeue();
					} else if (status != 200) {
						if (status == RemoteInterface.NO_CONNECTIVITY_STATUS) {
							hasNetwork_ = false;
							handleFailure();
							if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE)) {
								requestQueue_.dequeue();
							}
							Log.i("BranchSDK", "Branch API Error: " + "poor network connectivity. Please try again later.");
						} else {
							retryLastRequest();
						}
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS)) {
						processReferralCounts(serverResponse);
						requestQueue_.dequeue();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_REWARDS)) {
						processRewardCounts(serverResponse);
						requestQueue_.dequeue();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_REWARD_HISTORY)) {
						processCreditHistory(serverResponse);
						requestQueue_.dequeue();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
						prefHelper_.setDeviceFingerPrintID(serverResponse.getObject().getString("device_fingerprint_id"));
						prefHelper_.setIdentityID(serverResponse.getObject().getString("identity_id"));
						prefHelper_.setUserURL(serverResponse.getObject().getString("link"));
						prefHelper_.setSessionID(serverResponse.getObject().getString("session_id"));
						prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
						
						if (prefHelper_.getIsReferrable() == 1) {
							if (serverResponse.getObject().has("data")) {
								String params = serverResponse.getObject().getString("data");
								prefHelper_.setInstallParams(params);
							} else {
								prefHelper_.setInstallParams(PrefHelper.NO_STRING_VALUE);
							}
						}
						
						if (serverResponse.getObject().has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getObject().getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}	
						if (serverResponse.getObject().has("data")) {
							String params = serverResponse.getObject().getString("data");
							prefHelper_.setSessionParams(params);
						} else {
							prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						}
						
						updateAllRequestsInQueue();
						
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (initSessionFinishedCallback_ != null) {
									initSessionFinishedCallback_.onInitFinished(getLatestReferringParams());
								}
							}
						});
						requestQueue_.dequeue();
						initFinished_ = true;
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
						prefHelper_.setSessionID(serverResponse.getObject().getString("session_id"));
						prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
						if (serverResponse.getObject().has("link_click_id")) {
							prefHelper_.setLinkClickID(serverResponse.getObject().getString("link_click_id"));
						} else {
							prefHelper_.setLinkClickID(PrefHelper.NO_STRING_VALUE);
						}
						
						if (prefHelper_.getIsReferrable() == 1) {
							if (serverResponse.getObject().has("data")) {
								String params = serverResponse.getObject().getString("data");
								prefHelper_.setInstallParams(params);
							} 
						}
						if (serverResponse.getObject().has("data")) {
							String params = serverResponse.getObject().getString("data");
							prefHelper_.setSessionParams(params);
						} else {
							prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						}
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (initSessionFinishedCallback_ != null) {
									initSessionFinishedCallback_.onInitFinished(getLatestReferringParams());
								}
							}
						});
						requestQueue_.dequeue();
						initFinished_ = true;
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
						final String url = serverResponse.getObject().getString("url");
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (linkCreateCallback_ != null) {
									linkCreateCallback_.onLinkCreate(url);
								}
							}
						});
						requestQueue_.dequeue();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_LOGOUT)) {
						prefHelper_.setSessionID(serverResponse.getObject().getString("session_id"));
						prefHelper_.setIdentityID(serverResponse.getObject().getString("identity_id"));
						prefHelper_.setUserURL(serverResponse.getObject().getString("link"));
						
						prefHelper_.setInstallParams(PrefHelper.NO_STRING_VALUE);
						prefHelper_.setSessionParams(PrefHelper.NO_STRING_VALUE);
						prefHelper_.setIdentity(PrefHelper.NO_STRING_VALUE);
						prefHelper_.clearUserValues();
						
						requestQueue_.dequeue();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_IDENTIFY)) {
						prefHelper_.setIdentityID(serverResponse.getObject().getString("identity_id"));
						prefHelper_.setUserURL(serverResponse.getObject().getString("link"));
						
						if (serverResponse.getObject().has("referring_data")) {
							String params = serverResponse.getObject().getString("referring_data");
							prefHelper_.setInstallParams(params);
						} 
						if (requestQueue_.getSize() > 0) {
							ServerRequest req = requestQueue_.peek();
							if (req.getPost() != null && req.getPost().has("identity")) {
								prefHelper_.setIdentity(req.getPost().getString("identity"));
							}
						}
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (initIdentityFinishedCallback_ != null) {
									initIdentityFinishedCallback_.onInitFinished(getFirstReferringParams());
								}
							}
						});
						requestQueue_.dequeue();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_CODE)) {
						processReferralCodeGet(serverResponse);
						requestQueue_.dequeue();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_VALIDATE_REFERRAL_CODE)) {
						processReferralCodeValidation(serverResponse);
						requestQueue_.dequeue();
					} else {
						requestQueue_.dequeue();
					}
					
					networkCount_ = 0;
					
					if (hasNetwork_) {
						processNextQueueItem();
					}
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}
	
	public interface BranchReferralInitListener {
		public void onInitFinished(JSONObject referringParams);
	}
	
	public interface BranchReferralStateChangedListener {
		public void onStateChanged(boolean changed);
	}
	
	public interface BranchLinkCreateListener {
		public void onLinkCreate(String url);
	}
	
	public interface BranchListResponseListener {
		public void onReceivingResponse(JSONArray list);
	}
	
	public enum CreditHistoryOrder {
	    kMostRecentFirst,
	    kLeastRecentFirst
	}
}
