var exec = require('cordova/exec');

var Branch = function(callback, app_key) {
	var execArgs = [];
	var me = this;
	if (app_key) { execArgs.push(app_key); }
	exec(
		function(r) { callback(me); },
		function(e) { callback(me); },
		"Branch", 
		"getInstance", 
		execArgs
	);
};

Branch.prototype.initUserSession = function(callback, is_referrable) {
	var execArgs = [];
	if (is_referrable) { execArgs.push(is_referrable); }
	exec(
		function(r) {
			if (callback) {
				if (r.data) { 
					callback(r.data); 
				} else {
					callback({ });
				}
			}
		},
		function(e) {
			if (callback) { callback({ }); }
		},
		"Branch", 
		"initUserSession", 
		execArgs
	);
};

Branch.prototype.getInstallReferringParams = function(callback) {
	exec(
		function(r) {
			if (r.data) { 
				callback(r.data); 
			} else {
				callback({ });
			}
		},
		function(e) {
			callback({ });
		},
		"Branch", 
		"getInstallReferringParams", 
		[]
	);
};

Branch.prototype.getReferringParams = function(callback) {
	exec(
		function(r) {
			if (r.data) { 
				callback(r.data); 
			} else {
				callback({ });
			}
		},
		function(e) {
			callback({ });
		},
		"Branch", 
		"getReferringParams", 
		[]
	);
};

Branch.prototype.hasIdentity = function(callback) {
	exec(
		function(r) {
			if (r.check) { 
				callback(r.check); 
			} else {
				callback(false);
			}
		},
		function(e) {
			callback(false);
		},
		"Branch", 
		"hasIdentity", 
		[]
	);
};

Branch.prototype.identifyUser = function(callback, identity) {
	var execArgs = [];
	if (identity) { execArgs.push(identity); }
	exec(
		function(r) {
			if (callback) {
				if (r.data) { 
					callback(r.data); 
				} else {
					callback({ });
				}
			}
		},
		function(e) {
			if (callback) { callback({ }); }
		},
		"Branch", 
		"identifyUser", 
		execArgs
	);
};

Branch.prototype.clearUser = function() {
	exec(
		function(r) { },
		function(e) { },
		"Branch", 
		"clearUser", 
		[]
	);
};

Branch.prototype.userCompletedAction = function(event, metadata) {
	var execArgs = [];
	if (event) { execArgs.push(event); }
	if (metadata) { execArgs.push(metadata); }
	exec(
		function(r) { },
		function(e) { },
		"Branch", 
		"userCompletedAction", 
		execArgs
	);
};

Branch.prototype.getShortUrl = function(callback, data, tag) {
	var execArgs = [];
	if (data) { execArgs.push(data); }
	if (tag) { execArgs.push(tag); }
	exec(
		function(r) {
			if (r.url) { 
				callback(r.url); 
			} else {
				callback("");
			}
		},
		function(e) { 
			callback("");
		},
		"Branch", 
		"getShortUrl", 
		execArgs
	);
};