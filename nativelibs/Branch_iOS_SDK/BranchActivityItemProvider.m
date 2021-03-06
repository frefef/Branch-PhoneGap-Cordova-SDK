//
//  BranchActivityItemProvider.m
//  Branch-TestBed
//
//  Created by Scott Hasbrouck on 1/28/15.
//  Copyright (c) 2015 Branch Metrics. All rights reserved.
//

#import "BranchActivityItemProvider.h"
#import "Branch.h"
#import "BNCSystemObserver.h"

@implementation BranchActivityItemProvider

- (id)initWithParams:(NSDictionary *)params
                 andTags:(NSArray *)tags
              andFeature:(NSString *)feature
                andStage:(NSString *)stage
                andAlias:(NSString *)alias {
    
    NSString *url = [[Branch getInstance] getLongURLWithParams:params andChannel:nil andTags:tags andFeature:feature andStage:stage andAlias:alias];
    
    self = [super initWithPlaceholderItem:url];
    
    if (self) {
        self.params = params;
        self.tags = tags;
        self.feature = feature;
        self.stage = stage;
        self.alias = alias;
        self.branchURL = url;
        self.semaphore = dispatch_semaphore_create(0);
    }
    
    return self;
}

- (id) item {
    if ([self.placeholderItem isKindOfClass:[NSString class]]) {
        
        NSString *channel = [BranchActivityItemProvider humanReadableChannelWithActivityType:self.activityType];
        
        __weak BranchActivityItemProvider *weakSelf = self;
        [[Branch getInstance] getShortURLWithParams:self.params andTags:self.tags andChannel:channel andFeature:self.feature andStage:self.stage andAlias:self.alias andCallback:^(NSString *url, NSError *err) {
            if (!err) {
                self.branchURL = url;
            }
            dispatch_semaphore_signal(weakSelf.semaphore);
        }];
        dispatch_semaphore_wait(self.semaphore, DISPATCH_TIME_FOREVER);
        return self.branchURL;
    }
    return self.placeholderItem;
}

// Human readable activity type string
+ (NSString *)humanReadableChannelWithActivityType:(NSString *)activityString {
    NSString *channel = activityString; //default
    
    // Set to a more human readible sting if we can identify it
    if (activityString == UIActivityTypeAssignToContact) {
        channel = @"assign_to_contact";
    } else if (activityString == UIActivityTypeCopyToPasteboard) {
        channel = @"pasteboard";
    } else if (activityString == UIActivityTypeMail) {
        channel = @"email";
    } else if (activityString == UIActivityTypeMessage) {
        channel = @"sms";
    } else if (activityString == UIActivityTypePostToFacebook) {
        channel = @"facebook";
    } else if (activityString == UIActivityTypePostToTwitter) {
        channel = @"twitter";
    } else if (activityString == UIActivityTypePostToWeibo) {
        channel = @"weibo";
    } else if (activityString == UIActivityTypePrint) {
        channel = @"print";
    } else if (activityString == UIActivityTypeSaveToCameraRoll) {
        channel = @"camera_roll";
    } else if ([BNCSystemObserver getOSVersion].integerValue >= 7) {
        if (activityString == UIActivityTypeAddToReadingList) {
            channel = @"reading_list";
        } else if (activityString == UIActivityTypeAirDrop) {
            channel = @"airdrop";
        } else if (activityString == UIActivityTypePostToFlickr) {
            channel = @"flickr";
        } else if (activityString == UIActivityTypePostToTencentWeibo) {
            channel = @"tencent_weibo";
        } else if (activityString == UIActivityTypePostToVimeo) {
            channel = @"vimeo";
        }
    }
    return channel;
}

@end
