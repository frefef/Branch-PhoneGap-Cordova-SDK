//
//  CDVBranch_SDK.h
//  Branch-SDK
//
//  Created by Alex Austin on 6/5/14.
//  Copyright (c) 2014 Branch Metrics. All rights reserved.
//
#import <Foundation/Foundation.h>
#import <Cordova/CDV.h>
#import "Branch.h"

@interface CDVBranch : CDVPlugin

// Exec API
- (void)getInstance:(CDVInvokedUrlCommand*)command;

- (void)initUserSession:(CDVInvokedUrlCommand*)command;

- (void)getInstallReferringParams:(CDVInvokedUrlCommand*)command;
- (void)getReferringParams:(CDVInvokedUrlCommand*)command;

- (void)hasIdentity:(CDVInvokedUrlCommand*)command;
- (void)identifyUser:(CDVInvokedUrlCommand*)command;
- (void)clearUser:(CDVInvokedUrlCommand*)command;

- (void)userCompletedAction:(CDVInvokedUrlCommand*)command;

- (void)getShortUrl:(CDVInvokedUrlCommand*)command;

@end
