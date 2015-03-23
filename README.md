

## Installation - Branch PhoneGap or Cordova SDK

### Available in Cordova

Branch is available through [Cordova](http://plugins.cordova.io/#/package/io.branchmetrics.branchreferral), to install it simply execute the following line in terminal, while in your project directory:

    cordova plugin add io.branchmetrics.branchreferral

### Register you app

You can sign up for your own app id at [https://dashboard.branch.io](https://dashboard.branch.io)

## Configuration (for tracking)

Ideally, you want to use our links any time you have an external link pointing to your app (share, invite, referral, etc) because:

1. Our dashboard can tell you where your installs are coming from
1. Our links are the highest possible converting channel to new downloads and users
1. You can pass that shared data across install to give new users a custom welcome or show them the content they expect to see

Our linking infrastructure will support anything you want to build. If it doesn't, we'll fix it so that it does: just reach out to alex@branch.io with requests.

### Initialize SDK And Register Deep Link Routing Function

Called when app first initializes a session, ideally in the app delegate. If you created a custom link with your own custom dictionary data, you probably want to know when the user session init finishes, so you can check that data. Think of this callback as your "deep link router". If your app opens with some data, you want to route the user depending on the data you passed in. Otherwise, send them to a generic install flow.

This deep link routing callback is called 100% of the time on init, with your link params or an empty dictionary if none present.

```js
var branch = window.Branch;
// Arguments
// arg1: Your app key can be retrieved on the [Settings](https://dashboard.branch.io/#/settings) page of the dashboard
// arg2: the callback to notify you that the instance has instantiated
branch.getInstance("Your app key here", function() {

    // call init session to start a user session and check if that user has been deep linked
    // Arguments
    // arg1: Tell us if this user is eligible to be considered referred - important for referral program integrations
    // arg2: The callback that will be executed when initialization is complete. The parameters returned here will be the key/value pair associated with the link the user 
    // clicked or empty if no link was clicked
    branch.initSession(true, function(params) {
        if (params) {
            console.log('Returned params: ' + JSON.stringify(params));

            // handle the deep link data
        }
    });
});

```

### Close the session

Close sesion must be called whenever the app goes into the background, as it tells the native library that on the next app open, it should check if a new link had been clicked. If you don't call it, you'll notice that the deep link parameters will not be delivered reliably.

```js
var branch = window.Branch;
branch.closeSession();  
```

#### Retrieve session (install or open) parameters

These session parameters will be available at any point later on with this command. If no params, the dictionary will be empty. This refreshes with every new session (app installs AND app opens)
```js
var branch = window.Branch;
branch.getLatestReferringParams(function(data) {
   // do something with the latest session parameters
});
```

#### Retrieve install (install only) parameters

If you ever want to access the original session params (the parameters passed in for the first install event only), you can use this line. This is useful if you only want to reward users who newly installed the app from a referral link or something.
```js
var branch = window.Branch;
branch.getFirstReferringParams(function(data) {
    // do something with the data associated with the first user referral 
});
```

### Persistent identities

Often, you might have your own user IDs, or want referral and event data to persist across platforms or uninstall/reinstall. It's helpful if you know your users access your service from different devices. This where we introduce the concept of an 'identity'.

To identify a user, just call:
```js
var branch = window.Branch;
branch.setIdentity("user_id_14512", null);
```

#### Logout

If you provide a logout function in your app, be sure to clear the user when the logout completes. This will ensure that all the stored parameters get cleared and all events are properly attributed to the right identity.

**Warning** this call will clear the referral credits and attribution on the device.

```objc
var branch = window.Branch;
branch.logout();
```

### Register custom events

```js
var branch = window.Branch;
branch.userCompletedAction("your_custom_event", null);
```

OR if you want to store some state with the event

```js
var branch = window.Branch;
branch.userCompletedAction("your_custom_event", { "time":135512331, "sessions":12 });
```

Some example events you might want to track:
```js
"complete_purchase"
"wrote_message"
"finished_level_ten"
```

## Generate Tracked, Deep Linking URLs (pass data across install and open)

### Shortened links

There are a bunch of options for creating these links. You can tag them for analytics in the dashboard, or you can even pass data to the new installs or opens that come from the link click. How awesome is that? You need to pass a callback for when you link is prepared (which should return very quickly, ~ 50 ms to process).

For more details on how to create links, see the [Branch link creation guide](https://github.com/BranchMetrics/Branch-Integration-Guides/blob/master/url-creation-guide.md)

```js
// associate data with a link
// you can access this data from any instance that installs or opens the app from this link (amazing...)

var params = {;
    "user": "Joe",
    "profile_pic": "https://s3-us-west-1.amazonaws.com/myapp/joes_pic.jpg",
    "description": "Joe likes long walks on the beach..."
}

// associate a url with a set of tags, channel, feature, and stage for better analytics.
// channel: null or examples: "facebook", "twitter", "text_message", etc
// feature: null or examples: "sharing", "referral", "unlock", etc

var branch = window.Branch;
branch.getShortUrl(params, "facebook", "sharing", function(url) {
   // share the URL on Facebook 
});
// The callback will return null if the link generation fails (or if the alias specified is aleady taken.)
```

There are other methods which exclude tag and data if you don't want to pass those. Explore Xcode's autocomplete functionality.

**Note**
You can customize the Facebook OG tags of each URL if you want to dynamically share content by using the following _optional keys in the data dictionary_:

| Key | Value
| --- | ---
| "$og_title" | The title you'd like to appear for the link in social media
| "$og_description" | The description you'd like to appear for the link in social media
| "$og_image_url" | The URL for the image you'd like to appear for the link in social media
| "$og_video" | The URL for the video 
| "$og_url" | The URL you'd like to appear
| "$og_app_id" | Your OG app ID. Optional and rarely used.

Also, you do custom redirection by inserting the following _optional keys in the dictionary_:

| Key | Value
| --- | ---
| "$desktop_url" | Where to send the user on a desktop or laptop. By default it is the Branch-hosted text-me service
| "$android_url" | The replacement URL for the Play Store to send the user if they don't have the app. _Only necessary if you want a mobile web splash_
| "$ios_url" | The replacement URL for the App Store to send the user if they don't have the app. _Only necessary if you want a mobile web splash_
| "$ipad_url" | Same as above but for iPad Store
| "$fire_url" | Same as above but for Amazon Fire Store
| "$blackberry_url" | Same as above but for Blackberry Store
| "$windows_phone_url" | Same as above but for Windows Store

You have the ability to control the direct deep linking of each link by inserting the following _optional keys in the dictionary_:

| Key | Value
| --- | ---
| "$deeplink_path" | The value of the deep link path that you'd like us to append to your URI. For example, you could specify "$deeplink_path": "radio/station/456" and we'll open the app with the URI "yourapp://radio/station/456?link_click_id=branch-identifier". This is primarily for supporting legacy deep linking infrastructure. 
| "$always_deeplink" | true or false. (default is not to deep link first) This key can be specified to have our linking service force try to open the app, even if we're not sure the user has the app installed. If the app is not installed, we fall back to the respective app store or $platform_url key. By default, we only open the app if we've seen a user initiate a session in your app from a Branch link (has been cookied and deep linked by Branch)

## Referral system rewarding functionality

In a standard referral system, you have 2 parties: the original user and the invitee. Our system is flexible enough to handle rewards for all users. Here are a couple example scenarios:

1) Reward the original user for taking action (eg. inviting, purchasing, etc)

2) Reward the invitee for installing the app from the original user's referral link

3) Reward the original user when the invitee takes action (eg. give the original user credit when their the invitee buys something)

These reward definitions are created on the dashboard, under the 'Reward Rules' section in the 'Referrals' tab on the dashboard.

Warning: For a referral program, you should not use unique awards for custom events and redeem pre-identify call. This can allow users to cheat the system.

### Get reward balance

Reward balances change randomly on the backend when certain actions are taken (defined by your rules), so you'll need to make an asynchronous call to retrieve the balance. Here is the syntax:

```js
var branch = window.Branch;
branch.loadRewards(function(changed) {
    // changed will indicate whether credits were updated
    if (changed) {
        // Pass the bucket name to retrieve the current balance of credits
        var credits = branch.getCredits("default");
    }
});
```

### Redeem all or some of the reward balance (store state)

We will store how many of the rewards have been deployed so that you don't have to track it on your end. In order to save that you gave the credits to the user, you can call redeem. Redemptions will reduce the balance of outstanding credits permanently.

```js
var branch = window.Branch;
branch.redeemRewards(5, "default");
```

### Get credit history

This call will retrieve the entire history of credits and redemptions from the individual user. To use this call, implement like so:

```js
var branch = window.Branch;
branch.getCreditHistory(function(history) {
    // retrieve and display the credit history
    // example transaction opbject below
});
```

The response will return an array that has been parsed from the following JSON:
```json
[
    {
        "transaction": {
                           "date": "2014-10-14T01:54:40.425Z",
                           "id": "50388077461373184",
                           "bucket": "default",
                           "type": 0,
                           "amount": 5
                       },
        "referrer": "12345678",
        "referree": null
    },
    {
        "transaction": {
                           "date": "2014-10-14T01:55:09.474Z",
                           "id": "50388199301710081",
                           "bucket": "default",
                           "type": 2,
                           "amount": -3
                       },
        "referrer": null,
        "referree": "12345678"
    }
]
```
**referrer**
: The id of the referring user for this credit transaction. Returns null if no referrer is involved. Note this id is the user id in developer's own system that's previously passed to Branch's identify user API call.

**referree**
: The id of the user who was referred for this credit transaction. Returns null if no referree is involved. Note this id is the user id in developer's own system that's previously passed to Branch's identify user API call.

**type**
: This is the type of credit transaction

1. _0_ - A reward that was added automatically by the user completing an action or referral
1. _1_ - A reward that was added manually
2. _2_ - A redemption of credits that occurred through our API or SDKs
3. _3_ - This is a very unique case where we will subtract credits automatically when we detect fraud