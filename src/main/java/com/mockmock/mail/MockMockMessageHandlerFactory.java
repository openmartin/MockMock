package com.mockmock.mail;

import com.google.common.eventbus.EventBus;
import com.mockmock.Settings;
import com.mockmock.dao.Store;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.subethamail.smtp.MessageContext;
import org.subethamail.smtp.MessageHandler;
import org.subethamail.smtp.MessageHandlerFactory;
import org.subethamail.smtp.RejectException;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

@Service
public class MockMockMessageHandlerFactory implements MessageHandlerFactory
{
    private EventBus eventBus;
	private Settings settings;
	private Store store;

    @Autowired
    public MockMockMessageHandlerFactory(EventBus eventBus)
    {
        this.eventBus = eventBus;
    }

	@Autowired
	public void setSettings(Settings settings)
	{
		this.settings = settings;
	}

	@Autowired
    public void setStore(Store store){
        this.store = store;
    }

	@Override
    public MessageHandler create(MessageContext messageContext)
    {
        return new MockMockHandler(messageContext);
    }

    class MockMockHandler implements MessageHandler
    {
        MessageContext context;
        MockMail mockMail;

        /**
         * Constructor
         * @param context MessageContext
         */
        public MockMockHandler(MessageContext context)
        {
            this.context = context;
            this.mockMail = new MockMail();

            // give the mockmail a unique id (currently its just a timestamp in ms)
            this.mockMail.setId(DateTime.now().getMillis());
        }

        /**
         * Called first, after the MAIL FROM during a SMTP exchange.
         * @param from String
         * @throws RejectException
         */
        @Override
        public void from(String from) throws RejectException
        {
            this.mockMail.setFrom(from);

            if(settings.getShowEmailInConsole())
            {
                System.out.println("FROM:" + from);
            }
            else
            {
                System.out.println("Email from " + from + " received.");
            }
        }

        /**
         * Called once for every RCPT TO during a SMTP exchange.
         * This will occur after a from() call.
         * @param recipient String
         * @throws RejectException
         */
        @Override
        public void recipient(String recipient) throws RejectException
        {
            this.mockMail.setTo(recipient);

            if(settings.getShowEmailInConsole())
            {
                System.out.println("RECIPIENT:" + recipient);
            }
        }

        /**
         * Called when the DATA part of the SMTP exchange begins.
         * @param data InputStream
         * @throws RejectException
         * @throws IOException
         */
        @Override
        public void data(InputStream data) throws RejectException, IOException
        {
            String rawMail = this.convertStreamToString(data);
            mockMail.setRawMail(rawMail);

            Session session = Session.getDefaultInstance(new Properties());
            InputStream is = new ByteArrayInputStream(rawMail.getBytes());

            try
            {
                MimeMessage message = new MimeMessage(session, is);
                mockMail.setSubject(message.getSubject());
                mockMail.setMimeMessage(message);

                Address[] allBcc = message.getRecipients(Message.RecipientType.BCC);
                ArrayList<String> allBCCStr = new ArrayList<String>();
                for (Address addr : allBcc) {
                    allBCCStr.add(addr.toString());
                }
                mockMail.setBcc(StringUtils.join(allBCCStr, ","));

                System.out.println("Subject: " + mockMail.getSubject());
                System.out.println("BCC: " + mockMail.getBcc());


                Object messageContent = message.getContent();
                if(messageContent instanceof Multipart)
                {
                    Multipart multipart = (Multipart) messageContent;
                    for (int i = 0; i < multipart.getCount(); i++)
                    {
                        BodyPart bodyPart = multipart.getBodyPart(i);
                        String contentType = bodyPart.getContentType();
                        contentType = contentType.replaceAll("\t|\r|\n", "");
                        //System.out.println(contentType);

                        if(contentType.matches("text/plain.*"))
                        {
                            mockMail.setBody(convertStreamToString(bodyPart.getInputStream()));
                        }
                        else if(contentType.matches("text/html.*"))
                        {
                            mockMail.setBodyHtml(convertStreamToString(bodyPart.getInputStream()));
                        }
                        else if(contentType.matches("multipart/related.*")){
                            // compound documents
                            Multipart contentMulti = (Multipart)bodyPart.getContent();
                            for (int j = 0; j < contentMulti.getCount(); j++){
                                BodyPart subPart = contentMulti.getBodyPart(i);
                                String subContentType = subPart.getContentType();
                                //System.out.println(subContentType);
                                String encoding = "UTF-8";

                                if(subContentType.matches("text/html.*")){
                                    String bodyHtml = IOUtils.toString(MimeUtility.decode(subPart.getInputStream(), "quoted-printable"), "utf-8");
                                    mockMail.setBodyHtml(bodyHtml);
                                }
                            }

                        } else if (contentType.matches("application/octet-stream.*") || contentType.matches("application/pdf")) {
                            // attachment
                            String strFileName = MimeUtility.decodeText(bodyPart.getFileName());
                            mockMail.setAttacheFileName(strFileName);
                            byte[] attachContent = IOUtils.toByteArray(bodyPart.getInputStream());
                            mockMail.setAttachment(attachContent);
                        }
                    }
                }
                else if(messageContent instanceof InputStream)
                {
                    InputStream mailContent = (InputStream) messageContent;
                    mockMail.setBody(convertStreamToString(mailContent));
                }
                else if(messageContent instanceof String)
                {
                    String contentType = message.getContentType();

                    if(contentType.matches("text/plain.*"))
                    {
                        mockMail.setBody(messageContent.toString());
                    }
                    else if(contentType.matches("text/html.*"))
                    {
                        mockMail.setBodyHtml(messageContent.toString());
                    }
                }
            }
            catch (MessagingException e)
            {
                e.printStackTrace();
            }

            if(settings.getShowEmailInConsole())
            {
                System.out.println("MAIL DATA");
                System.out.println("= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =");
                System.out.println(mockMail.getRawMail());
                System.out.println("= = = = = = = = = = = = = = = = = = = = = = = = = = = = = = =");
            }
        }

        @Override
        public void done()
        {
			// check if this email's "from" address matches one in the filtered list
			if(settings.getFilterFromEmailAddresses().contains(mockMail.getFrom()))
			{
				System.out.println("Skipping email, because From address '" + mockMail.getFrom() + "' matches filter");
				return;
			}

			// check if this email's "to" address matches one in the filtered list
			if(settings.getFilterToEmailAddresses().contains(mockMail.getTo()))
			{
				System.out.println("Skipping email, because To address '" + mockMail.getTo() + "' matches filter");
				return;
			}

            // set the received date
            mockMail.setReceivedTime(DateTime.now().getMillis());

            if(settings.getShowEmailInConsole())
            {
                System.out.println("Finished");
            }

            //eventBus.post(mockMail);
            // don't use eventbus, use MySQL database
            mockMail.setReceive_time(new Date());
            store.addMail(mockMail);
        }

        /**
         * Converts given input stream to String
         * @param is InputStream
         * @return String
         */
        protected String convertStreamToString(InputStream is)
        {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder stringBuilder = new StringBuilder();

            String line;
            try
            {
                while ((line = reader.readLine()) != null)
                {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            return stringBuilder.toString();
        }
    }
}
