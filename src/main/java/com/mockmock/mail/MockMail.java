package com.mockmock.mail;

import org.apache.commons.io.IOUtils;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

public class MockMail implements Comparable<MockMail>
{
    private long id;
    private String from;
    private String to;
    private String subject;
    private String body;
    private String bodyHtml;
    private String rawMail;
    private MimeMessage mimeMessage;
    private long receivedTime;
    private byte[] attachment;
    private String attacheFileName;
    private Date receive_time;

    public long getId()
    {
        return id;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public String getFrom()
    {
        return from;
    }
    
    public void setFrom(String from)
    {
        this.from = from;
    }
    
    public String getTo()
    {
        return to;
    }

    public void setTo(String to)
    {
        this.to = to;
    }
    
    public String getSubject() 
    {
        return subject;
    }
    
    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getBody()
    {
        return body;
    }

    public void setBody(String body)
    {
        this.body = body;
    }

    public String getRawMail()
    {
        return rawMail;
    }

    public void setRawMail(String rawMail)
    {
        this.rawMail = rawMail;
    }

    public String getBodyHtml() 
    {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) 
    {
        this.bodyHtml = bodyHtml;
    }

    public MimeMessage getMimeMessage()
    {
        return mimeMessage;
    }

    public void setMimeMessage(MimeMessage mimeMessage)
    {
        this.mimeMessage = mimeMessage;
    }

    @Override
    public int compareTo(MockMail o)
    {
        long receivedTime = this.getReceivedTime();
        long receivedTime2 = o.getReceivedTime();

        long diff = receivedTime - receivedTime2;
        return (int) diff;
    }

    public long getReceivedTime()
    {
        return receivedTime;
    }

    public void setReceivedTime(long receivedTime)
    {
        this.receivedTime = receivedTime;
    }

    public byte[] getAttachment() {
        return attachment;
    }

    public void setAttachment(byte[] attachment) {
        this.attachment = attachment;
    }

    public String getAttacheFileName() {
        return attacheFileName;
    }

    public void setAttacheFileName(String attacheFileName) {
        this.attacheFileName = attacheFileName;
    }

    public Date getReceive_time() {
        return receive_time;
    }

    public void setReceive_time(Date receive_time) {
        this.receive_time = receive_time;
    }

    public void loadMail(String rawMail) throws IOException {
        this.setRawMail(rawMail);

        Session session = Session.getDefaultInstance(new Properties());
        InputStream is = new ByteArrayInputStream(rawMail.getBytes());

        try
        {
            MimeMessage message = new MimeMessage(session, is);
            this.setSubject(message.getSubject());
            this.setMimeMessage(message);

            Object messageContent = message.getContent();
            if(messageContent instanceof Multipart)
            {
                Multipart multipart = (Multipart) messageContent;
                for (int i = 0; i < multipart.getCount(); i++)
                {
                    BodyPart bodyPart = multipart.getBodyPart(i);
                    String contentType = bodyPart.getContentType();
                    contentType = contentType.replaceAll("\t|\r|\n", "");
                    System.out.println(contentType);

                    if(contentType.matches("text/plain.*"))
                    {
                        this.setBody(IOUtils.toString(bodyPart.getInputStream()));
                    }
                    else if(contentType.matches("text/html.*"))
                    {
                        this.setBodyHtml(IOUtils.toString(bodyPart.getInputStream()));
                    }
                    else if(contentType.matches("multipart/related.*")){
                        // compound documents
                        Multipart contentMulti = (Multipart)bodyPart.getContent();
                        for (int j = 0; j < contentMulti.getCount(); j++){
                            BodyPart subPart = contentMulti.getBodyPart(i);
                            String subContentType = subPart.getContentType();
                            System.out.println(subContentType);
                            String encoding = "UTF-8";

                            if(subContentType.matches("text/html.*")){
                                String bodyHtml = IOUtils.toString(MimeUtility.decode(subPart.getInputStream(), "quoted-printable"), "utf-8");
                                this.setBodyHtml(bodyHtml);
                            }
                        }

                    }else if(contentType.matches("application/octet-stream.*")){
                        // attachment
                        String strFileName = MimeUtility.decodeText(bodyPart.getFileName());
                        this.setAttacheFileName(strFileName);
                        byte[] attachContent = IOUtils.toByteArray(bodyPart.getInputStream());
                        this.setAttachment(attachContent);
                    }
                }
            }
            else if(messageContent instanceof InputStream)
            {
                InputStream mailContent = (InputStream) messageContent;
                this.setBody(IOUtils.toString(mailContent));
            }
            else if(messageContent instanceof String)
            {
                String contentType = message.getContentType();

                if(contentType.matches("text/plain.*"))
                {
                    this.setBody(messageContent.toString());
                }
                else if(contentType.matches("text/html.*"))
                {
                    this.setBodyHtml(messageContent.toString());
                }
            }
        }
        catch (MessagingException e)
        {
            e.printStackTrace();
        }
    }
}
