package com.hiresense.api.auth.email;

public interface EmailSender {

    void send(String to, String subject, String textBody);
}
