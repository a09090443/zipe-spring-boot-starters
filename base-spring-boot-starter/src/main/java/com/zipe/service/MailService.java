package com.zipe.service;

import com.zipe.model.Mail;

import javax.mail.MessagingException;

public interface MailService {

	void setInitData() throws MessagingException;

	void sendEmail(Mail mail);

	void simpleMailSend(Mail mail);

	void attachedSend(Mail mail) throws MessagingException;

	void richContentSend(Mail mail) throws MessagingException;

	void sendBatchMailWithFile(Mail mail) throws Exception;
}
