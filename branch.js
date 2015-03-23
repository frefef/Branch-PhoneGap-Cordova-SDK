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

Branch.prototype.setDebug = function() {
	exec(
		function(r) { },
		function(e) { },
		"Branch", 
		"setDebug", 
		[]
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

Branch.prototype.loadActionCounts = function(callback) {
	exec(
		function(r) {
			if (r.changed) { 
				callback(r.changed); 
			} else {
				callback(false);
			}
		},
		function(e) { 
			callback(false);
		},
		"Branch", 
		"loadActionCounts", 
		execArgs
	);
};

Branch.prototype.loadRewards = function(callback) {
	exec(
		function(r) {
			if (r.changed) { 
				callback(r.changed); 
			} else {
				callback(false);
			}
		},
		function(e) { 
			callback(false);
		},
		"Branch", 
		"loadRewards", 
		execArgs
	);
};

Branch.prototype.getCreditHistory = function(callback) {
	exec(
		function(r) {
			if (r.list) { 
				callback(r.list); 
			} else {
				callback([]);
			}
		},
		function(e) { 
			callback([]);
		},
		"Branch", 
		"getCreditHistory", 
		execArgs
	);
};

Branch.prototype.getCredits = function(bucket, callback) {
	var execArgs = Array.prototype.slice.apply(arguments);
	callback = execArgs.pop();
	exec(
		function(r) {
			if (r.credits) { 
				callback(r.credits); 
			} else {
				callback(0);
			}
		},
		function(e) {
			callback(0);
		},
		"Branch", 
		"getCredits", 
		execArgs
	);
};

Branch.prototype.redeemRewards = function(amount, bucket) {
	var execArgs = Array.prototype.slice.apply(arguments);
	exec(
		function(r) { },
		function(e) { },
		"Branch", 
		"redeemRewards", 
		execArgs
	);
};

Branch.prototype.getTotalCountsForAction = function(action, callback) {
	var execArgs = Array.prototype.slice.apply(arguments);
	callback = execArgs.pop();
	exec(
		function(r) {
			if (r.count) { 
				callback(r.count); 
			} else {
				callback(0);
			}
		},
		function(e) {
			callback(0);
		},
		"Branch", 
		"getTotalCountsForAction", 
		execArgs
	);
};

Branch.prototype.getUniqueCountsForAction = function(action, callback) {
	var execArgs = Array.prototype.slice.apply(arguments);
	callback = execArgs.pop();
	exec(
		function(r) {
			if (r.count) { 
				callback(r.count); 
			} else {
				callback(0);
			}
		},
		function(e) {
			callback(0);
		},
		"Branch", 
		"getUniqueCountsForAction", 
		execArgs
	);
};

var branch = new Branch();
module.exports = branch;
