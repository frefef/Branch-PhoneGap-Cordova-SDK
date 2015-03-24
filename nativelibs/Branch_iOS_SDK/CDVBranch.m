//
//  Branch_SDK.m
//  Branch-SDK
//
//  Created by Alex Austin on 6/5/14.
//  Copyright (c) 2014 Branch Metrics. All rights reserved.
//

#import "CDVBranch.h"
#import "Branch.h"

@interface CDVBranch()

@property (strong, nonatomic) Branch *branch;

@end

@implementation CDVBranch

- (void)getInstance:(CDVInvokedUrlCommand*)command {
    NSArray *args = command.arguments;
    if ([args count]) {
        if (!self.branch) {
            self.branch = [Branch getInstance:(NSString *)[args objectAtIndex:0]];
        }
    } else {
        if (!self.branch) {
            self.branch = [Branch getInstance];
        }
    }
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void)setDebug:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    [Branch setDebug];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void)initSession:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }

    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    NSArray *args = command.arguments;
    if ([args count]) {
        [self.branch initSession:[[args objectAtIndex:0] boolValue] andRegisterDeepLinkHandler:^(NSDictionary *data, NSError *error) {
            [retParams setObject:data forKey:@"data"];
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
        }];
    } else {
        [self.branch initSessionAndRegisterDeepLinkHandler:^(NSDictionary *data, NSError *error) {
            [retParams setObject:data forKey:@"data"];
            [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
        }];
    }
}

- (void)closeSession:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    [self.branch closeSession];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void)getFirstReferringParams:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [retParams setObject:[self.branch getFirstReferringParams] forKey:@"data"];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
}
- (void)getLatestReferringParams:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [retParams setObject:[self.branch getLatestReferringParams] forKey:@"data"];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
}

- (void)setIdentity:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [self.branch setIdentity:[command.arguments objectAtIndex:0] withCallback:^(NSDictionary *params, NSError *error) {
        [retParams setObject:params forKey:@"data"];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
    }];
}
- (void)logout:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    [self.branch logout];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void)userCompletedAction:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSString *action = nil;
    NSDictionary *metadata = nil;
    for (id object in command.arguments) {
        if ([object isKindOfClass:[NSString class]]) {
            action = object;
        } else if ([object isKindOfClass:[NSDictionary class]]) {
            metadata = object;
        }
    }
    if (action) {
        [self.branch userCompletedAction:action withState:metadata];
    }
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void)getShortUrl:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    NSString *channel = nil;
    NSString *feature = nil;
    int currentStringParam = 0;
    NSDictionary *data = nil;
    for (id object in command.arguments) {
        if ([object isKindOfClass:[NSString class]]) {
            if (currentStringParam == 0) {
                channel = object;
                currentStringParam = 1;
            } else {
                feature = object;
            }
        } else if ([object isKindOfClass:[NSDictionary class]]) {
            data = object;
        }
    }

    [self.branch getShortURLWithParams:data andChannel:channel andFeature:feature andCallback:^(NSString *url, NSError *error) {
        [retParams setObject:url forKey:@"url"];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
    }];
}

- (void)getContentUrl:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    NSString *channel = nil;
    NSDictionary *data = nil;
    for (id object in command.arguments) {
        if ([object isKindOfClass:[NSString class]]) {
            channel = object;
        } else if ([object isKindOfClass:[NSDictionary class]]) {
            data = object;
        }
    }

    [self.branch getContentUrlWithParams:data andChannel:channel andCallback:^(NSString *url, NSError *error) {
        [retParams setObject:url forKey:@"url"];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
    }];
}

- (void)loadActionCounts:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [self.branch loadActionCountsWithCallback:^(BOOL changed, NSError *error) {
        [retParams setObject:[NSNumber numberWithBool:changed] forKey:@"changed"];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
    }];
}

- (void)loadRewards:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [self.branch loadRewardsWithCallback:^(BOOL changed, NSError *error) {
        [retParams setObject:[NSNumber numberWithBool:changed] forKey:@"changed"];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
    }];
}

- (void)getCreditHistory:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [self.branch getCreditHistoryWithCallback:^(NSArray *list, NSError *error) {
        [retParams setObject:list forKey:@"list"];
        [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
    }];
}

- (void)getCredits:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSString *bucket = nil;
    if ([command.arguments count] && [[command.arguments objectAtIndex:0] isKindOfClass:[NSString class]]) {
        bucket = [command.arguments objectAtIndex:0];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [retParams setObject:[NSNumber numberWithInteger:[self.branch getCreditsForBucket:bucket]] forKey:@"credits"];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
}

- (void)redeemRewards:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSInteger credits = 0;
    NSString *bucket = nil;
    for (id object in command.arguments) {
        if ([object isKindOfClass:[NSString class]]) {
            bucket = object;
        } else {
            credits = [object integerValue];
        }
    }
    [self.branch redeemRewards:credits forBucket:bucket];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK] callbackId:command.callbackId];
}

- (void)getTotalCountsForAction:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSString *action = nil;
    if ([command.arguments count] && [[command.arguments objectAtIndex:0] isKindOfClass:[NSString class]]) {
        action = [command.arguments objectAtIndex:0];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [retParams setObject:[NSNumber numberWithInteger:[self.branch getTotalCountsForAction:action]] forKey:@"count"];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
}

- (void)getUniqueCountsForAction:(CDVInvokedUrlCommand*)command {
    if (!self.branch) {
        self.branch = [Branch getInstance];
    }
    NSString *action = nil;
    if ([command.arguments count] && [[command.arguments objectAtIndex:0] isKindOfClass:[NSString class]]) {
        action = [command.arguments objectAtIndex:0];
    }
    NSMutableDictionary *retParams = [[NSMutableDictionary alloc] init];
    [retParams setObject:[NSNumber numberWithInteger:[self.branch getUniqueCountsForAction:action]] forKey:@"count"];
    [self.commandDelegate sendPluginResult:[CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:retParams] callbackId:command.callbackId];
}

@end
