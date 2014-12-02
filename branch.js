var exec = require('cordova/exec');

function Branch() { }

Branch.prototype.getInstance = function(app_key, callback) {
	var execArgs = [];
	var self = this;
	if (typeof app_key == 'function') { callback = app_key; }
	else { execArgs.push(app_key); }
	exec(
		function(r) { callback(self); },
		function(e) { callback(self); },
		"Branch", 
		"getInstance", 
		execArgs
	);
};

Branch.prototype.initSession = function(is_referrable, callback) {
	var execArgs = [];
	if (typeof is_referrable == 'function') { callback = is_referrable; }
	else { execArgs.push(is_referrable); }
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
		"initSession", 
		execArgs
	);
};

Branch.prototype.getFirstReferringParams = function(callback) {
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
		"getFirstReferringParams", 
		[]
	);
};

Branch.prototype.getLatestReferringParams = function(callback) {
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
		"getLatestReferringParams", 
		[]
	);
};

Branch.prototype.closeSession = function() {
	exec(
		function(r) { },
		function(e) { },
		"Branch", 
		"closeSession", 
		[]
	);
};

Branch.prototype.setIdentity = function(identity, callback) {
	var execArgs = [];
	if (typeof identity == 'function') { callback = identity; }
	else { execArgs.push(identity); }
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
		"setIdentity", 
		execArgs
	);
};

Branch.prototype.logout = function() {
	exec(
		function(r) { },
		function(e) { },
		"Branch", 
		"logout", 
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

Branch.prototype.getShortUrl = function(data, channel, feature, callback) {
	var execArgs = Array.prototype.slice.apply(arguments);
	callback = execArgs.pop();
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

Branch.prototype.getContentUrl = function(data, channel, callback) {
	var execArgs = Array.prototype.slice.apply(arguments);
	callback = execArgs.pop();
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
		"getContentUrl", 
		execArgs
	);
};

var branch = new Branch();
module.exports = branch;
