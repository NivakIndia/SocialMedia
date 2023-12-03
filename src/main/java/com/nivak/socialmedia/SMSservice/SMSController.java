package com.nivak.socialmedia.SMSservice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SMSController {

    @Autowired
    private SMSService smsService;

    @PostMapping("/send-sms")
    public String sendSMS(@RequestParam String toPhoneNumber, @RequestParam String message) {
        smsService.sendSMS(toPhoneNumber, message);
        return "SMS sent successfully!";
    }
}


