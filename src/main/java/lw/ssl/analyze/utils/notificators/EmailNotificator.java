package lw.ssl.analyze.utils.notificators;

import lw.ssl.analyze.utils.PropertyFilesHelper;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletContext;
import java.util.Properties;

/**
 * Created by zmushko_m on 21.04.2016.
 */
public class EmailNotificator {
    private static final String EMAIL_PROPERTIES_SERVLET_CONTENT_PATH = "/WEB-INF/properties/email.properties";

    public static void notificate(String messageBody, String subject, ServletContext servletContext) {
        if (messageBody != null && messageBody != "") {

            try {
                final Properties props = PropertyFilesHelper.getPropertyByPath(EMAIL_PROPERTIES_SERVLET_CONTENT_PATH, servletContext);

                if (!props.isEmpty()) {
                    Session session = Session.getDefaultInstance(props,
                            new javax.mail.Authenticator() {
                                protected PasswordAuthentication getPasswordAuthentication() {
                                    return new PasswordAuthentication(props.getProperty("mail.user"), props.getProperty("mail.password"));
                                }
                            });

                    Message msg = new MimeMessage(session);
                    msg.setFrom(new InternetAddress(props.getProperty("mail.from")));

                    String mailToEMails = props.getProperty("mail.to");
                    String[] eMails = mailToEMails.split(",");
                    InternetAddress[] internetAddresses = new InternetAddress[eMails.length];
                    for (int i = 0; i < eMails.length; i++) {
                        internetAddresses[i] = new InternetAddress(eMails[i].trim());
                    }

                    msg.addRecipients(Message.RecipientType.TO, internetAddresses);
                    if (subject != null && subject != "") {
                        msg.setSubject(subject);
                    }
                    msg.setText(messageBody);

                    Transport.send(msg);
                }
            } catch (AddressException e) {
                e.printStackTrace();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}
