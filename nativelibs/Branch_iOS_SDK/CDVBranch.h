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

- (void)initSession:(CDVInvokedUrlCommand*)command;
- (void)closeSession:(CDVInvokedUrlCommand*)command;

- (void)getFirstReferringParams:(CDVInvokedUrlCommand*)command;
- (void)getLatestReferringParams:(CDVInvokedUrlCommand*)command;

- (void)setIdentity:(CDVInvokedUrlCommand*)command;
- (void)logout:(CDVInvokedUrlCommand*)command;

- (void)userCompletedAction:(CDVInvokedUrlCommand*)command;

- (void)getShortUrl:(CDVInvokedUrlCommand*)command;
- (void)getContentUrl:(CDVInvokedUrlCommand*)command;

@end
