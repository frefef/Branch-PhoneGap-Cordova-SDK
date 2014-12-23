

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
You can customize the Facebook OG tags of each URL if you want to dynamically share content by using the following optional keys in the params dictionary:
```js
"$og_app_id"
"$og_title"
"$og_description"
"$og_image_url"
"$og_video"
"$og_url"
```

Also, you do custom redirection by inserting the following optional keys in the dictionary. For example, if you want to send users on the desktop to a page on your website, insert the $desktop_url with that URL value
```js
"$desktop_url"
"$android_url"
"$ios_url"
"$ipad_url"
```
