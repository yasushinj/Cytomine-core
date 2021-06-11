package be.cytomine.utils

/*
* Copyright (c) 2009-2021. Authors: see NOTICE file.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import be.cytomine.Exception.MiddlewareException
import grails.util.Holders
import org.springframework.core.io.FileSystemResource
import org.springframework.mail.MailAuthenticationException
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper

import javax.mail.MessagingException
import javax.mail.AuthenticationFailedException
import javax.mail.internet.MimeMessage


class CytomineMailService {

    //static final String NO_REPLY_EMAIL = "noreply@revealbio.com"
    static final String NO_REPLY_EMAIL = "noreply@revealbio.com"

    static transactional = false


    def send(String from, String[] to, String cc, String subject, String message, def attachment = null) {

        String defaultEmail = Holders.getGrailsApplication().config.grails.notification.email

        if (!from) from = defaultEmail

        log.info "defaultEmail : $defaultEmail"
        log.info "from : $from"
        //log.info "NO_REPLY_EMAIL $NO_REPLY_EMAIL"
        //String ID = Holders.getGrailsApplication().config.grails.notification.password
        //log.info "ID  : $ID"

        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.starttls.required","true");
        props.put("mail.smtp.host",Holders.getGrailsApplication().config.grails.notification.smtp.host);
        props.put("mail.smtp.port",Holders.getGrailsApplication().config.grails.notification.smtp.port);
        props.put("mail.smtp.auth", "true" );
        props.put("mail.transport.protocol", "smtp");

        //Create Mail Sender
        def sender = new JavaMailSenderImpl()
        sender.setJavaMailProperties(props)
        sender.setUsername(defaultEmail)
        sender.setPassword(Holders.getGrailsApplication().config.grails.notification.password)
        sender.setDefaultEncoding("UTF-8")
        MimeMessage mail = sender.createMimeMessage()
        MimeMessageHelper helper = new MimeMessageHelper(mail, true)

        helper.setReplyTo("support@revealbio.com")
        helper.setFrom(from)
        helper.setTo(to)
        //helper.setCc(cc)
        helper.setSubject(subject)
        helper.setText("",message)
        attachment?.each {
            helper.addInline((String) it.cid, new FileSystemResource((File)it.file))
        }

        //log.info "send $mail"
        //log.info "sender $sender"
        try {
            sender.send(mail)
        } catch (AuthenticationFailedException | MessagingException | MailAuthenticationException e) {
            log.error "can't send email $mail (MessagingException)"
            throw new MiddlewareException(e.getMessage())
        }
    }
}
