package io.branch.referral;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class Branch {
	public static final String FEATURE_TAG_SHARE = "share";
	public static final String FEATURE_TAG_REFERRAL = "referral";
	public static final String FEATURE_TAG_INVITE = "invite";
	public static final String FEATURE_TAG_DEAL = "deal";
	public static final String FEATURE_TAG_GIFT = "gift";

	public static final String REDEEM_CODE = "$redeem_code";
	public static final String REFERRAL_BUCKET_DEFAULT = "default";
	public static final String REFERRAL_CODE_TYPE = "credit";
	public static final int REFERRAL_CREATION_SOURCE_SDK = 2;
	public static final String REFERRAL_CODE = "referral_code";
	
	public static final String REDIRECT_DESKTOP_URL = "$desktop_url";
	public static final String REDIRECT_ANDROID_URL = "$android_url";
	public static final String REDIRECT_IOS_URL = "$ios_url";
	public static final String REDIRECT_IPAD_URL = "$ipad_url";
	public static final String REDIRECT_FIRE_URL = "$fire_url";
	public static final String REDIRECT_BLACKBERRY_URL = "$blackberry_url";
	public static final String REDIRECT_WINDOWNS_PHONE_URL = "$windows_phone_url";
	
	public static final String OG_TITLE = "$og_title";
	public static final String OG_DESC = "$og_description";
	public static final String OG_IMAGE_URL = "$og_image_url";
	public static final String OG_VIDEO = "$og_video";
	public static final String OG_URL = "$og_url";
	public static final String OG_APP_ID = "$og_app_id";
	
	public static final String DEEPLINK_PATH = "$deeplink_path";
	public static final String ALWAYS_DEEPLINK = "$always_deeplink";
	
	public static final int REFERRAL_CODE_LOCATION_REFERREE = 0;
	public static final int REFERRAL_CODE_LOCATION_REFERRING_USER = 2;
	public static final int REFERRAL_CODE_LOCATION_BOTH = 3;

	public static final int REFERRAL_CODE_AWARD_UNLIMITED = 1;
	public static final int REFERRAL_CODE_AWARD_UNIQUE = 0;

	public static final int LINK_TYPE_UNLIMITED_USE = 0;
	public static final int LINK_TYPE_ONE_TIME_USE = 1;

	private static final int SESSION_KEEPALIVE = 100;
	private static final int PREVENT_CLOSE_TIMEOUT = 100;

	private static Branch branchReferral_;
	private boolean isInit_;

	private BranchReferralInitListener initSessionFinishedCallback_;
	private BranchReferralInitListener initIdentityFinishedCallback_;
	private BranchReferralStateChangedListener stateChangedCallback_;
	private BranchLinkCreateListener linkCreateCallback_;
	private BranchListResponseListener creditHistoryCallback_;
	private BranchReferralInitListener getReferralCodeCallback_;
	private BranchReferralInitListener validateReferralCodeCallback_;
	private BranchReferralInitListener applyReferralCodeCallback_;

	private BranchRemoteInterface kRemoteInterface_;
	private PrefHelper prefHelper_;
	private SystemObserver systemObserver_;
	private Context context_;

	final Object lock;
	private Timer closeTimer;
	private Timer rotateCloseTimer;
	private boolean keepAlive_;

	private Semaphore serverSema_;
	private ServerRequestQueue requestQueue_;
	private int networkCount_;
	private int retryCount_;
	
	private boolean initNotStarted_;
	private boolean initFinished_;
	private boolean initFailed_;
	private boolean hasNetwork_;
	private boolean lastRequestWasInit_;
	
	private Handler debugHandler_;
	private SparseArray<String> debugListenerInitHistory_;
	private OnTouchListener debugOnTouchListener_;
	
	private Map<BranchLinkData, String> linkCache_;
	
	private ScheduledFuture<?> appListingSchedule_;

	private Branch(Context context) {
		prefHelper_ = PrefHelper.getInstance(context);
		kRemoteInterface_ = new BranchRemoteInterface(context);
		systemObserver_ = new SystemObserver(context);
		kRemoteInterface_.setNetworkCallbackListener(new ReferralNetworkCallback());
		requestQueue_ = ServerRequestQueue.getInstance(context);
		serverSema_ = new Semaphore(1);
		closeTimer = new Timer();
		rotateCloseTimer = new Timer();
		lock = new Object();
		initFinished_ = false;
		initFailed_ = false;
		lastRequestWasInit_ = true;
		keepAlive_ = false;
		isInit_ = false;
		initNotStarted_ = true;
		networkCount_ = 0;
		hasNetwork_ = true;
		debugListenerInitHistory_ = new SparseArray<String>();
		debugHandler_ = new Handler();
		debugOnTouchListener_ = retrieveOnTouchListener();
		linkCache_ = new HashMap<BranchLinkData, String>();
	}

	@Deprecated
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

			String appKey = branchReferral_.prefHelper_.getAppKey();
	        if (appKey == null || appKey.equalsIgnoreCase(PrefHelper.NO_STRING_VALUE)) {
	        	Log.i("BranchSDK", "Branch Warning: Please enter your Branch App Key in your project's res/values/strings.xml!");
	        }
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

    public void setRetryCount(int retryCount) {
		if (prefHelper_ != null && retryCount > 0) {
			prefHelper_.setRetryCount(retryCount);
		}
	}
	
	public void setRetryInterval(int retryInterval) {
		if (prefHelper_ != null && retryInterval > 0) {
			prefHelper_.setRetryInterval(retryInterval);
		}
	}
	
	public void setNetworkTimeout(int timeout) {
		if (prefHelper_ != null && timeout > 0) {
			prefHelper_.setTimeout(timeout);
		}
	}

	// if you want to flag debug, call this before initUserSession
	public void setDebug() {
		prefHelper_.setExternDebug();
	}
	
	public void disableAppList() {
		prefHelper_.disableExternAppListing();
	}
	
	// Note: smart session - we keep session alive for two seconds
	// if there's further Branch API call happening within the two seconds, we then don't close the session;
	// otherwise, we close the session after two seconds
	// Call this method if you don't want this smart session feature
	public void disableSmartSession() {
		prefHelper_.disableSmartSession();
	}

	public boolean initSession(BranchReferralInitListener callback) {
		initSession(callback, (Activity)null);
		return false;
	}
	public boolean initSession(BranchReferralInitListener callback, Activity activity) {
		if (systemObserver_.getUpdateState() == 0 && !hasUser()) {
			prefHelper_.setIsReferrable();
		} else {
			prefHelper_.clearIsReferrable();
		}
		initUserSessionInternal(callback, activity);
		return false;
	}

	public boolean initSession(BranchReferralInitListener callback, Uri data) {
		return initSession(callback, data, null);
	}

	public boolean initSession(BranchReferralInitListener callback, Uri data, Activity activity) {
		boolean uriHandled = false;
		if (data != null && data.isHierarchical()) {
			if (data.getQueryParameter("link_click_id") != null) {
				uriHandled = true;
				prefHelper_.setLinkClickIdentifier(data.getQueryParameter("link_click_id"));
			}
		}
		initSession(callback, activity);
		return uriHandled;
	}

	public boolean initSession() {
		return initSession((Activity)null);
	}
	public boolean initSession(Activity activity) {
		return initSession(null, activity);
	}
	
	public boolean initSessionWithData(Uri data) {
		return initSessionWithData(data, null);
	}

	public boolean initSessionWithData(Uri data, Activity activity) {
		boolean uriHandled = false;
		if (data != null && data.isHierarchical()) {
			if (data.getQueryParameter("link_click_id") != null) {
				uriHandled = true;
				prefHelper_.setLinkClickIdentifier(data.getQueryParameter("link_click_id"));
			}
		}
		initSession(null, activity);
		return uriHandled;
	}

	public boolean initSession(boolean isReferrable) {
		return initSession(null, isReferrable, (Activity)null);
	}
	public boolean initSession(boolean isReferrable, Activity activity) {
		return initSession(null, isReferrable, activity);
	}

	public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Uri data) {
		return initSession(callback, isReferrable, data, null);
	}

	public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Uri data, Activity activity) {
		boolean uriHandled = false;
		if (data != null && data.isHierarchical()) {
			if (data.getQueryParameter("link_click_id") != null) {
				uriHandled = true;
				prefHelper_.setLinkClickIdentifier(data.getQueryParameter("link_click_id"));
			}
		}
		initSession(callback, isReferrable, activity);
		return uriHandled;
	}

	public boolean initSession(BranchReferralInitListener callback, boolean isReferrable) {
		return initSession(callback, isReferrable, (Activity)null);
	}
	public boolean initSession(BranchReferralInitListener callback, boolean isReferrable, Activity activity) {
		if (isReferrable) {
			this.prefHelper_.setIsReferrable();
		} else {
			this.prefHelper_.clearIsReferrable();
		}
		initUserSessionInternal(callback, activity);
		return false;
	}

	private void initUserSessionInternal(BranchReferralInitListener callback, Activity activity) {
		initSessionFinishedCallback_ = callback;
		lastRequestWasInit_ = true;
		initNotStarted_ = false;
		initFailed_ = false;
		if (!isInit_) {
			isInit_ = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					initializeSession();
				}
			}).start();
		} else {
			boolean installOrOpenInQueue = requestQueue_.containsInstallOrOpen();
			if (hasUser() && hasSession() && !installOrOpenInQueue) {
				if (callback != null)
					callback.onInitFinished(new JSONObject(), null);
				clearCloseTimer();
				keepAlive();
			} else {
				if (!installOrOpenInQueue) {
					new Thread(new Runnable() {
						@Override
						public void run() {
							initializeSession();
						}
					}).start();
				} else {
					new Thread(new Runnable() {
						@Override
						public void run() {
							requestQueue_.moveInstallOrOpenToFront(null, networkCount_);
							processNextQueueItem();
						}
					}).start();
				}
			}
		}
		
		if (activity != null && debugListenerInitHistory_.get(System.identityHashCode(activity)) == null) {
			debugListenerInitHistory_.put(System.identityHashCode(activity), "init");
			View view = activity.getWindow().getDecorView().findViewById(android.R.id.content);
			if (view != null) { 
				view.setOnTouchListener(debugOnTouchListener_);
			}
		}
	}
	
	private OnTouchListener retrieveOnTouchListener() {
		if (debugOnTouchListener_ == null) {
			debugOnTouchListener_ = new OnTouchListener() {
				class KeepDebugConnectionTask extends TimerTask {
			        public void run() {
			            if (!prefHelper_.keepDebugConnection()) {
			            	debugHandler_.post(_longPressed);
			            }
			        }
			    }
				
				Runnable _longPressed = new Runnable() {
					private boolean started = false;
					private Timer timer;
				
				    public void run() {
				    	debugHandler_.removeCallbacks(_longPressed);
				        if (!started) {
				        	Log.i("Branch Debug","======= Start Debug Session =======");
				        	prefHelper_.setDebug();
				        	timer = new Timer();
				        	timer.scheduleAtFixedRate(new KeepDebugConnectionTask(), new Date(), 20000);
				        } else {
				        	Log.i("Branch Debug","======= End Debug Session =======");
				        	prefHelper_.clearDebug();
				        	timer.cancel();
				        	timer = null;
				        }
				        this.started = !this.started;
				    }   
				};
				
				@Override
				public boolean onTouch(View v, MotionEvent ev) {
					int pointerCount = ev.getPointerCount();
					final int actionPeformed = ev.getAction();
					switch (actionPeformed & MotionEvent.ACTION_MASK) {
					case MotionEvent.ACTION_DOWN:
						if (systemObserver_.isSimulator()) {
							debugHandler_.postDelayed(_longPressed, PrefHelper.DEBUG_TRIGGER_PRESS_TIME);
						}
				        break;
				    case MotionEvent.ACTION_MOVE:
				        break;
				    case MotionEvent.ACTION_CANCEL:
				    	debugHandler_.removeCallbacks(_longPressed);
				        break;
				    case MotionEvent.ACTION_UP:
				    	v.performClick();
				    	debugHandler_.removeCallbacks(_longPressed);
				        break;
					case MotionEvent.ACTION_POINTER_DOWN:
						if (pointerCount == PrefHelper.DEBUG_TRIGGER_NUM_FINGERS) {
							debugHandler_.postDelayed(_longPressed, PrefHelper.DEBUG_TRIGGER_PRESS_TIME);
						}
						break;
					}
					return true;
				}
			};
		}
		return debugOnTouchListener_;
	}

	
	
	public void closeSession() {
		if (prefHelper_.getSmartSession()) {
			if (keepAlive_) {
				return;
			}
	
			// else, real close
			synchronized(lock) {
				clearCloseTimer();
				rotateCloseTimer.schedule(new TimerTask() {
					@Override
					public void run() {
						executeClose();
					}
				}, PREVENT_CLOSE_TIMEOUT);
			}
		} else {
			executeClose();
		}
		
		if (prefHelper_.getExternAppListing()) {
			if (appListingSchedule_ == null) {
				scheduleListOfApps();
			}
		}
	}
	
	private void executeClose() {
		isInit_ = false;
		lastRequestWasInit_ = false;
		initNotStarted_ = true;
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
					if (!requestQueue_.containsClose()) {
						ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE, null);
						requestQueue_.enqueue(req);
						if (initFinished_ || !hasNetwork_) {
							processNextQueueItem();
						} else if (initFailed_ || initNotStarted_) {
							handleFailure(req);
						}
					}
				}
			}).start();
		}
	}

	public void setIdentity(String userId, BranchReferralInitListener callback) {
		initIdentityFinishedCallback_ = callback;
		setIdentity(userId);
	}

	public void setIdentity(final String userId) {
		if (userId == null || userId.length() == 0 || userId.equals(prefHelper_.getIdentity())) {
			return;
		}

		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("session_id", prefHelper_.getSessionID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
						post.put("link_click_id", prefHelper_.getLinkClickID());
					}
					post.put("identity", userId);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_IDENTIFY, post);
				requestQueue_.enqueue(req);
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
				}
			}
		}).start();
	}

	public void logout() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("session_id", prefHelper_.getSessionID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
						post.put("link_click_id", prefHelper_.getLinkClickID());
					}
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_LOGOUT, post);
				requestQueue_.enqueue(req);
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
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
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS, null);
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
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
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REWARDS, null);
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
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
				int creditsToRedeem;
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
						post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
						post.put("session_id", prefHelper_.getSessionID());
						if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
							post.put("link_click_id", prefHelper_.getLinkClickID());
						}
						post.put("bucket", bucket);
						post.put("amount", creditsToRedeem);
					} catch (JSONException ex) {
						ex.printStackTrace();
						return;
					}
					ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_REDEEM_REWARDS, post);
					requestQueue_.enqueue(req);
					if (initFinished_ || !hasNetwork_) {
						lastRequestWasInit_ = false;
						processNextQueueItem();
					} else if (initFailed_ || initNotStarted_) {
						handleFailure(req);
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
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("session_id", prefHelper_.getSessionID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
						post.put("link_click_id", prefHelper_.getLinkClickID());
					}
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
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REWARD_HISTORY, post);
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
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
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("session_id", prefHelper_.getSessionID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
						post.put("link_click_id", prefHelper_.getLinkClickID());
					}
					post.put("event", action);
					if (metadata != null)
						post.put("metadata", filterOutBadCharacters(metadata));
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION, post);
				requestQueue_.enqueue(req);
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
				}		
			}
		}).start();
	}

	public void userCompletedAction(final String action) {
		userCompletedAction(action, null);
	}

	public JSONObject getFirstReferringParams() {
		String storedParam = prefHelper_.getInstallParams();
		return convertParamsStringToDictionary(storedParam);
	}

	public JSONObject getLatestReferringParams() {
		String storedParam = prefHelper_.getSessionParams();
		return convertParamsStringToDictionary(storedParam);
	}
	
	public String getShortUrlSync() {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, null, null, null, stringifyParams(null), null, false);
	}

	public String getShortUrlSync(JSONObject params) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, null, null, null, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getReferralUrlSync(String channel, JSONObject params) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getReferralUrlSync(Collection<String> tags, String channel, JSONObject params) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getContentUrlSync(String channel, JSONObject params) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, FEATURE_TAG_SHARE, null, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getContentUrlSync(Collection<String> tags, String channel, JSONObject params) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, FEATURE_TAG_SHARE, null, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getShortUrlSync(String channel, String feature, String stage, JSONObject params) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getShortUrlSync(String alias, String channel, String feature, String stage, JSONObject params) {
		return generateShortLink(alias, LINK_TYPE_UNLIMITED_USE, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getShortUrlSync(int type, String channel, String feature, String stage, JSONObject params) {
		return generateShortLink(null, type, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
	}
	
	public String getShortUrlSync(String channel, String feature, String stage, JSONObject params, int duration) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, duration, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getShortUrlSync(Collection<String> tags, String channel, String feature, String stage, JSONObject params) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getShortUrlSync(String alias, Collection<String> tags, String channel, String feature, String stage, JSONObject params) {
		return generateShortLink(alias, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getShortUrlSync(int type, Collection<String> tags, String channel, String feature, String stage, JSONObject params) {
		return generateShortLink(null, type, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
	}

	public String getShortUrlSync(Collection<String> tags, String channel, String feature, String stage, JSONObject params, int duration) {
		return generateShortLink(null, LINK_TYPE_UNLIMITED_USE, duration, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), null, false);
	}
	
	public void getShortUrl(BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, null, null, null, stringifyParams(null), callback, true);
	}

	public void getShortUrl(JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, null, null, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getReferralUrl(String channel, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getReferralUrl(Collection<String> tags, String channel, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, FEATURE_TAG_REFERRAL, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getContentUrl(String channel, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, FEATURE_TAG_SHARE, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getContentUrl(Collection<String> tags, String channel, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, FEATURE_TAG_SHARE, null, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getShortUrl(String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getShortUrl(String alias, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(alias, LINK_TYPE_UNLIMITED_USE, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getShortUrl(int type, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, type, 0, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}
	
	public void getShortUrl(String channel, String feature, String stage, JSONObject params, int duration, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, duration, null, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getShortUrl(Collection<String> tags, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getShortUrl(String alias, Collection<String> tags, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(alias, LINK_TYPE_UNLIMITED_USE, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getShortUrl(int type, Collection<String> tags, String channel, String feature, String stage, JSONObject params, BranchLinkCreateListener callback) {
		generateShortLink(null, type, 0, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}
	
	public void getShortUrl(Collection<String> tags, String channel, String feature, String stage, JSONObject params, int duration, BranchLinkCreateListener callback) {
		generateShortLink(null, LINK_TYPE_UNLIMITED_USE, duration, tags, channel, feature, stage, stringifyParams(filterOutBadCharacters(params)), callback, true);
	}

	public void getReferralCode(BranchReferralInitListener callback) {
		getReferralCodeCallback_ = callback;

		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("session_id", prefHelper_.getSessionID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
						post.put("link_click_id", prefHelper_.getLinkClickID());
					}
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_CODE, post);
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
				}
			}
		}).start();
	}

	public void getReferralCode(final int amount, BranchReferralInitListener callback) {
		this.getReferralCode(null, amount, null, REFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
	}

	public void getReferralCode(final String prefix, final int amount, BranchReferralInitListener callback) {
		this.getReferralCode(prefix, amount, null, REFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
	}

	public void getReferralCode(final int amount, final Date expiration, BranchReferralInitListener callback) {
		this.getReferralCode(null, amount, expiration, REFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
	}

	public void getReferralCode(final String prefix, final int amount, final Date expiration, BranchReferralInitListener callback) {
		this.getReferralCode(prefix, amount, expiration, REFERRAL_BUCKET_DEFAULT, REFERRAL_CODE_AWARD_UNLIMITED, REFERRAL_CODE_LOCATION_REFERRING_USER, callback);
	}

	public void getReferralCode(final String prefix, final int amount, final int calculationType, final int location, BranchReferralInitListener callback) {
		this.getReferralCode(prefix, amount, null, REFERRAL_BUCKET_DEFAULT, calculationType, location, callback);
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
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("session_id", prefHelper_.getSessionID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
						post.put("link_click_id", prefHelper_.getLinkClickID());
					}
					post.put("calculation_type", calculationType);
					post.put("location", location);
					post.put("type", REFERRAL_CODE_TYPE);
					post.put("creation_source", REFERRAL_CREATION_SOURCE_SDK);
					post.put("amount", amount);
					post.put("bucket", bucket != null ? bucket : REFERRAL_BUCKET_DEFAULT);
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
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_CODE, post);
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
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
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("session_id", prefHelper_.getSessionID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
						post.put("link_click_id", prefHelper_.getLinkClickID());
					}
					post.put("referral_code", code);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_VALIDATE_REFERRAL_CODE, post);
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
				}
			}
		}).start();
	}

	public void applyReferralCode(final String code, final BranchReferralInitListener callback) {
		applyReferralCodeCallback_ = callback;

		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					post.put("identity_id", prefHelper_.getIdentityID());
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("session_id", prefHelper_.getSessionID());
					if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
						post.put("link_click_id", prefHelper_.getLinkClickID());
					}
					post.put("referral_code", code);
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_APPLY_REFERRAL_CODE, post);
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
				}
			}
		}).start();
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
	
	private String generateShortLink(final String alias, final int type, final int duration, final Collection<String> tags, final String channel, final String feature, final String stage, final String params, BranchLinkCreateListener callback, boolean async) {
		linkCreateCallback_ = callback;
		if (hasUser()) {
			final BranchLinkData linkPost = new BranchLinkData();
			try {
				linkPost.put("app_id", prefHelper_.getAppKey());
				linkPost.put("identity_id", prefHelper_.getIdentityID());
				linkPost.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
				linkPost.put("session_id", prefHelper_.getSessionID());
				if (!prefHelper_.getLinkClickID().equals(PrefHelper.NO_STRING_VALUE)) {
					linkPost.put("link_click_id", prefHelper_.getLinkClickID());
				}

				linkPost.putType(type);
				linkPost.putDuration(duration);
				linkPost.putTags(tags);
				linkPost.putAlias(alias);
				linkPost.putChannel(channel);
				linkPost.putFeature(feature);
				linkPost.putStage(stage);
				linkPost.putParams(params);
				
			} catch (JSONException ex) {
				ex.printStackTrace();
			}
			
			if (linkCache_.containsKey(linkPost)) {
				String url = linkCache_.get(linkPost);
				if (linkCreateCallback_ != null) {
					linkCreateCallback_.onLinkCreate(url, null);
				}
				return url;
			} else {
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL, linkPost);
				if (async) {
					generateShortLinkAsync(req);
				} else {
					return generateShortLinkSync(req);
				}
			}
		}
		return null;
	}
	
	private String generateShortLinkSync(ServerRequest req) {
		if (!initFailed_ && initFinished_ && hasNetwork_) {
			ServerResponse response = kRemoteInterface_.createCustomUrlSync(req.getPost());
			String url = prefHelper_.getUserURL();
			if (response.getStatusCode() == 200) {
				try {
					url = response.getObject().getString("url");
					if (response.getLinkData() != null) {
						linkCache_.put(response.getLinkData(), url);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
			return url;
		} else {
			Log.i("BranchSDK", "Branch Warning: User session has not been initialized");
		}
		return null;
	}
	
	private void generateShortLinkAsync(final ServerRequest req) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				} else if (initFailed_ || initNotStarted_) {
					handleFailure(req);
				}
			}
		}).start();
	}
	
	private JSONObject filterOutBadCharacters(JSONObject inputObj) {
		JSONObject filteredObj = new JSONObject();
		if (inputObj != null) {
			Iterator<?> keys = inputObj.keys();
			while (keys.hasNext()) {
				String key = (String) keys.next();
				try {
					if (inputObj.has(key) && inputObj.get(key).getClass().equals(String.class)) {
						filteredObj.put(key, inputObj.getString(key).replace("\n", "\\n").replace("\r", "\\r").replace("\"", "\\\""));
					} else if (inputObj.has(key)) {
						filteredObj.put(key, inputObj.get(key));
					}
				} catch(JSONException ignore) {
					
				}	
			}
		}
		return filteredObj;
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
	
	private void scheduleListOfApps() {
		ScheduledThreadPoolExecutor scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1);
		Runnable periodicTask = new Runnable(){
            @Override
            public void run() {
            	SystemObserver sysObserver = new SystemObserver(context_);
				JSONObject post = new JSONObject();
				try {
					post.put("app_id", prefHelper_.getAppKey());
					if (!sysObserver.getOS().equals(SystemObserver.BLANK))
						post.put("os", sysObserver.getOS());
					post.put("device_fingerprint_id", prefHelper_.getDeviceFingerPrintID());
					post.put("apps_data", sysObserver.getListOfApps());
				} catch (JSONException ex) {
					ex.printStackTrace();
					return;
				}
				ServerRequest req = new ServerRequest(BranchRemoteInterface.REQ_TAG_SEND_APP_LIST, post);
				if (!initFailed_) {
					requestQueue_.enqueue(req);
				}
				if (initFinished_ || !hasNetwork_) {
					lastRequestWasInit_ = false;
					processNextQueueItem();
				}
            }
        };
        
        Date date = new Date();
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(date); 
        
        int days = Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK);	// days to Saturday
        int hours = 2 - calendar.get(Calendar.HOUR_OF_DAY);	// hours to 2am, can be negative
        if (days == 0 && hours < 0) {
        	days = 7;
        }
        int interval = 7 * 24 * 60 * 60;
        
        appListingSchedule_ = scheduler.scheduleAtFixedRate(periodicTask, (days * 24 + hours) * 60 * 60, interval, TimeUnit.SECONDS);
	}
	
	private void processNextQueueItem() {
		try {
			serverSema_.acquire();
			if (networkCount_ == 0 && requestQueue_.getSize() > 0) {
				networkCount_ = 1;
				ServerRequest req = requestQueue_.peek();
				serverSema_.release();

				if (!req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL) && !hasUser()) {
	                Log.i("BranchSDK", "Branch Error: User session has not been initialized!");
	                networkCount_ = 0;
					handleFailure(requestQueue_.getSize()-1);
	                return;
	            }
				
				if (!req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE) && !req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
					keepAlive();
					clearCloseTimer();
				}

				if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL)) {
					kRemoteInterface_.registerInstall(PrefHelper.NO_STRING_VALUE, prefHelper_.isDebug());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
					kRemoteInterface_.registerOpen(prefHelper_.isDebug());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS) && hasSession()) {
					kRemoteInterface_.getReferralCounts();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_SEND_APP_LIST) && hasSession()) {
					kRemoteInterface_.registerListOfApps(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARDS) && hasSession()) {
					kRemoteInterface_.getRewards();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REDEEM_REWARDS) && hasSession()) {
					kRemoteInterface_.redeemRewards(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARD_HISTORY) && hasSession()) {
					kRemoteInterface_.getCreditHistory(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_COMPLETE_ACTION) && hasSession()) {
					kRemoteInterface_.userCompletedAction(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL) && hasSession()) {
					kRemoteInterface_.createCustomUrl(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_IDENTIFY) && hasSession()) {
					kRemoteInterface_.identifyUser(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE) && hasSession()) {
					kRemoteInterface_.registerClose();
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_LOGOUT) && hasSession()) {
					kRemoteInterface_.logoutUser(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_CODE) && hasSession()) {
					kRemoteInterface_.getReferralCode(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_VALIDATE_REFERRAL_CODE) && hasSession()) {
					kRemoteInterface_.validateReferralCode(req.getPost());
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_APPLY_REFERRAL_CODE) && hasSession()) {
					kRemoteInterface_.applyReferralCode(req.getPost());
				}
			} else {
				serverSema_.release();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void handleFailure(int index) {
		ServerRequest req;
		if (index >= requestQueue_.getSize()) {
			req = requestQueue_.peekAt(requestQueue_.getSize()-1);
		} else {
			req = requestQueue_.peekAt(index);
		}
		handleFailure(req);
	}
	
	private void handleFailure(final ServerRequest req) {
		if (req == null)
			return;
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_INSTALL) || req.getTag().equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
					if (initSessionFinishedCallback_ != null) {
						JSONObject obj = new JSONObject();
						try {
							obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
						} catch (JSONException ex) {
							ex.printStackTrace();
						}
						initSessionFinishedCallback_.onInitFinished(obj, new BranchInitError());
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_COUNTS)) {
					if (stateChangedCallback_ != null) {
						if (initNotStarted_)
							stateChangedCallback_.onStateChanged(false, new BranchNotInitError());
						else
							stateChangedCallback_.onStateChanged(false, new BranchGetReferralsError());
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARDS)) {
					if (stateChangedCallback_ != null) {
						if (initNotStarted_)
							stateChangedCallback_.onStateChanged(false, new BranchNotInitError());
						else
							stateChangedCallback_.onStateChanged(false, new BranchGetCreditsError());
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REWARD_HISTORY)) {
					if (creditHistoryCallback_ != null) {
						if (initNotStarted_)
							creditHistoryCallback_.onReceivingResponse(null, new BranchNotInitError());
						else
							creditHistoryCallback_.onReceivingResponse(null, new BranchGetCreditHistoryError());
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
					if (linkCreateCallback_ != null) {
						String failedUrl = null;
						if (!prefHelper_.getUserURL().equals(PrefHelper.NO_STRING_VALUE)) {
							failedUrl = prefHelper_.getUserURL();
						}
						if (initNotStarted_)
							linkCreateCallback_.onLinkCreate(null, new BranchNotInitError());
						else
							linkCreateCallback_.onLinkCreate(failedUrl, new BranchCreateUrlError());
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_IDENTIFY)) {
					if (initIdentityFinishedCallback_ != null) {
						JSONObject obj = new JSONObject();
						try {
							obj.put("error_message", "Trouble reaching server. Please try again in a few minutes");
						} catch (JSONException ex) {
							ex.printStackTrace();
						}
						if (initNotStarted_)
							initIdentityFinishedCallback_.onInitFinished(obj, new BranchNotInitError());
						else
							initIdentityFinishedCallback_.onInitFinished(obj, new BranchSetIdentityError());
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_GET_REFERRAL_CODE)) {
					if (getReferralCodeCallback_ != null) {
						if (initNotStarted_)
							getReferralCodeCallback_.onInitFinished(null, new BranchNotInitError());
						else
							getReferralCodeCallback_.onInitFinished(null, new BranchGetReferralCodeError());
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_VALIDATE_REFERRAL_CODE)) {
					if (validateReferralCodeCallback_ != null) {
						if (initNotStarted_)
							validateReferralCodeCallback_.onInitFinished(null, new BranchNotInitError());
						else
							validateReferralCodeCallback_.onInitFinished(null, new BranchValidateReferralCodeError());
					}
				} else if (req.getTag().equals(BranchRemoteInterface.REQ_TAG_APPLY_REFERRAL_CODE)) {
					if (applyReferralCodeCallback_ != null) {
						if (initNotStarted_)
							applyReferralCodeCallback_.onInitFinished(null, new BranchNotInitError());
						else
							applyReferralCodeCallback_.onInitFinished(null, new BranchApplyReferralCodeError());
					}
				}
			}
		});
	}

	private void retryLastRequest() {
		retryCount_ = retryCount_ + 1;
		if (retryCount_ > prefHelper_.getRetryCount()) {
			handleFailure(0);
			requestQueue_.dequeue();
			retryCount_ = 0;
		} else {
			try {
				Thread.sleep(prefHelper_.getRetryInterval());
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
						String key = (String) keys.next();
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

	private void clearCloseTimer() {
		if (rotateCloseTimer == null)
			return;
		rotateCloseTimer.cancel();
		rotateCloseTimer.purge();
		rotateCloseTimer = new Timer();
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
		synchronized(lock) {
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
			String key = (String) keys.next();

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
					stateChangedCallback_.onStateChanged(finUpdateListener, null);
				}
			}
		});
	}

	private void processRewardCounts(ServerResponse resp) {
		boolean updateListener = false;
		Iterator<?> keys = resp.getObject().keys();
		while (keys.hasNext()) {
			String key = (String) keys.next();

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
					stateChangedCallback_.onStateChanged(finUpdateListener, null);
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
					creditHistoryCallback_.onReceivingResponse(resp.getArray(), null);
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
						JSONObject json;
						BranchDuplicateReferralCodeError error = null;
						// check if a valid referral code json is returned
						if (!resp.getObject().has(REFERRAL_CODE)) {
							json = new JSONObject();
							json.put("error_message", "Failed to get referral code");
							error = new BranchDuplicateReferralCodeError();
						} else {
							json = resp.getObject();
						}
						getReferralCodeCallback_.onInitFinished(json, error);
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
						JSONObject json;
						BranchInvalidReferralCodeError error = null;
						// check if a valid referral code json is returned
						if (!resp.getObject().has(REFERRAL_CODE)) {
							json = new JSONObject();
							json.put("error_message", "Invalid referral code");
							error = new BranchInvalidReferralCodeError();
						} else {
							json = resp.getObject();
						}
						validateReferralCodeCallback_.onInitFinished(json, error);
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	private void processReferralCodeApply(final ServerResponse resp) {
		Handler mainHandler = new Handler(context_.getMainLooper());
		mainHandler.post(new Runnable() {
			@Override
			public void run() {
				if (applyReferralCodeCallback_ != null) {
					try {
						JSONObject json;
						BranchInvalidReferralCodeError error = null;
						// check if a valid referral code json is returned
						if (!resp.getObject().has(REFERRAL_CODE)) {
							json = new JSONObject();
							json.put("error_message", "Invalid referral code");
							error = new BranchInvalidReferralCodeError();
						} else {
							json = resp.getObject();
						}
						applyReferralCodeCallback_.onInitFinished(json, error);
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

					if (status == 409) {
						if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
							Handler mainHandler = new Handler(context_.getMainLooper());
							mainHandler.post(new Runnable() {
								@Override
								public void run() {
									if (linkCreateCallback_ != null) {
										linkCreateCallback_.onLinkCreate(null, new BranchDuplicateUrlError());
									}
								}
							});
						} else {
							Log.i("BranchSDK", "Branch API Error: Conflicting resource error code from API");
							handleFailure(0);
						}
						requestQueue_.dequeue();
					} else if (status >= 400 && status < 500) {
						if (serverResponse.getObject().has("error") && serverResponse.getObject().getJSONObject("error").has("message")) {
							Log.i("BranchSDK", "Branch API Error: " + serverResponse.getObject().getJSONObject("error").getString("message"));
						}
						if (lastRequestWasInit_ && !initFailed_) {
							initFailed_ = true;
							for (int i = 0; i < requestQueue_.getSize()-1; i++) {
								handleFailure(i);
							}
						}
						handleFailure(requestQueue_.getSize()-1);
						requestQueue_.dequeue();
					} else if (status != 200) {
						if (status == RemoteInterface.NO_CONNECTIVITY_STATUS) {
							hasNetwork_ = false;
							handleFailure(lastRequestWasInit_ ? 0 : requestQueue_.getSize()-1);
							if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_CLOSE)) {
								requestQueue_.dequeue();
							}
							Log.i("BranchSDK", "Branch API Error: poor network connectivity. Please try again later.");
						} else if (status == RemoteInterface.NO_API_KEY_STATUS) {
							handleFailure(lastRequestWasInit_ ? 0 : requestQueue_.getSize()-1);
							Log.i("BranchSDK", "Branch API Error: Please enter your Branch App Key in your project's res/values/strings.xml first!");
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
									initSessionFinishedCallback_.onInitFinished(getLatestReferringParams(), null);
								}
							}
						});
						requestQueue_.dequeue();
						initFinished_ = true;
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_REGISTER_OPEN)) {
						prefHelper_.setSessionID(serverResponse.getObject().getString("session_id"));
						prefHelper_.setDeviceFingerPrintID(serverResponse.getObject().getString("device_fingerprint_id"));
						prefHelper_.setLinkClickIdentifier(PrefHelper.NO_STRING_VALUE);
						if (serverResponse.getObject().has("identity_id")) {
							prefHelper_.setIdentityID(serverResponse.getObject().getString("identity_id"));
						}
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
									initSessionFinishedCallback_.onInitFinished(getLatestReferringParams(), null);
								}
							}
						});
						requestQueue_.dequeue();
						initFinished_ = true;
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_SEND_APP_LIST)) {
						prefHelper_.clearSystemReadStatus();
						requestQueue_.dequeue();
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_GET_CUSTOM_URL)) {
						final String url = serverResponse.getObject().getString("url");
						
						// cache the link
						linkCache_.put(serverResponse.getLinkData(), url);
						
						Handler mainHandler = new Handler(context_.getMainLooper());
						mainHandler.post(new Runnable() {
							@Override
							public void run() {
								if (linkCreateCallback_ != null) {
									linkCreateCallback_.onLinkCreate(url, null);
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
									initIdentityFinishedCallback_.onInitFinished(getFirstReferringParams(), null);
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
					} else if (requestTag.equals(BranchRemoteInterface.REQ_TAG_APPLY_REFERRAL_CODE)) {
						processReferralCodeApply(serverResponse);
						requestQueue_.dequeue();
					} else {
						requestQueue_.dequeue();
					}

					networkCount_ = 0;
					
					if (hasNetwork_ && !initFailed_) {
						lastRequestWasInit_ = false;
						processNextQueueItem();
					}
				} catch (JSONException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	public interface BranchReferralInitListener {
		public void onInitFinished(JSONObject referringParams, BranchError error);
	}

	public interface BranchReferralStateChangedListener {
		public void onStateChanged(boolean changed, BranchError error);
	}

	public interface BranchLinkCreateListener {
		public void onLinkCreate(String url, BranchError error);
	}

	public interface BranchListResponseListener {
		public void onReceivingResponse(JSONArray list, BranchError error);
	}
	
	

	public enum CreditHistoryOrder {
		kMostRecentFirst, kLeastRecentFirst
	}
	
	public class BranchInitError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble initializing Branch. Check network connectivity or that your app key is valid";
		}
	}
	
	public class BranchGetReferralsError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble retrieving referral counts. Check network connectivity and that you properly initialized";
		}
	}
	
	public class BranchGetCreditsError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble retrieving user credits. Check network connectivity and that you properly initialized";
		}
	}
	
	public class BranchGetCreditHistoryError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble retrieving user credit history. Check network connectivity and that you properly initialized";
		}
	}
	
	public class BranchCreateUrlError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble creating a URL. Check network connectivity and that you properly initialized";
		}
	}
	
	public class BranchDuplicateUrlError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble creating a URL with that alias. If you want to reuse the alias, make sure to submit the same properties for all arguments and that the user is the same owner";
		}
	}
	
	public class BranchSetIdentityError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble setting the user alias. Check network connectivity and that you properly initialized";
		}
	}
	
	public class BranchGetReferralCodeError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble retrieving the referral code. Check network connectivity and that you properly initialized";
		}
	}
	
	public class BranchValidateReferralCodeError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble validating the referral code. Check network connectivity and that you properly initialized";
		}
	}
	
	public class BranchInvalidReferralCodeError extends BranchError {
		@Override
		public String getMessage() {
			return "That Branch referral code was invalid";
		}
	}
	
	public class BranchDuplicateReferralCodeError extends BranchError {
		@Override
		public String getMessage() {
			return "That Branch referral code is already in use";
		}
	}
	
	public class BranchApplyReferralCodeError extends BranchError {
		@Override
		public String getMessage() {
			return "Trouble applying the referral code. Check network connectivity and that you properly initialized";
		}
	}
	
	public class BranchNotInitError extends BranchError {
		@Override
		public String getMessage() {
			return "Did you forget to call init? Make sure you init the session before making Branch calls";
		}
	}
}
