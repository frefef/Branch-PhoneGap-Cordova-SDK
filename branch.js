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

Branch.prototype.initUserSession = function(is_referrable, callback) {
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

Branch.prototype.closeSession = function() {
	exec(
		function(r) { },
		function(e) { },
		"Branch", 
		"closeSession", 
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

Branch.prototype.identifyUser = function(identity, callback) {
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
