"use strict";
const prefix = "sg1";
const mailDomain = "@cloudlabhk.com";
module.exports.configure = {
    projectId: "MarkingAccelerator",
    prefix: prefix,
    awsAccount: "ive",
    errorReportEmail: "cytmp-marking@yahoo.com",
    region: "ap-southeast-1",
    sesRegion: "us-west-2",
    ruleSetName: "default-rule-set",
    senderEmail: "noreply" + mailDomain,
    errorEmail: "error" + mailDomain,
    sourceBucket: "ite3101assignment"
};