package com.adobe.cq;
 
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Reference;
 
 
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set; 
import java.util.Properties;
 
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.component.ComponentContext;
 
//import MBean API
import javax.management.MBeanServerConnection; 
import javax.management.MBeanServer ; 
import java.lang.management.ManagementFactory ; 
import javax.management.ObjectName;
 
//Java Mail API
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
 
/**
 * Just a simple DS Component
 */
@Component(metatype=true)
@Service
public class SimpleDSComponent implements Runnable {
     
     
    /** Default log. */
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
     
    private BundleContext bundleContext;
     
    @Reference
    private Scheduler scheduler ; 
     
    public void run() {
        log.info("Running...");
    }
     
    protected void activate(ComponentContext ctx) {
        this.bundleContext = ctx.getBundleContext();
         
         
        //Schedule a Sling Job to invoke an MBean operation to obtain number of Stale Workflow items
      //case 3: with fireJobAt(): executes the job at a specific date (date of deployment + delay of 30 seconds)
        String jobName3 = "case3";
         
         
                 
        String schedulingExpression = "0 15 10 ? * MON-FRI"; //10:15am every Monday, Tuesday, Wednesday, Thursday and Friday
 
        final Date fireDate = new Date();
       
        Map<String, Serializable> config3 = new HashMap<String, Serializable>();
        final Runnable job = new Runnable() {
            public void run() {
 
                int staleItems = checkStaleItems(); 
                 
                //if greater than 6 - email AEM admins
                if (staleItems > 6)
                {
                    sendMail(staleItems); 
                }
                 
            }
        };
        try {
            //Add the Job
            this.scheduler.addJob("myJob", job, null, schedulingExpression, true);
             
        } catch (Exception e) {
            job.run();
        }
         
         
    }
     
    protected void deactivate(ComponentContext ctx) {
        this.bundleContext = null;
    }
     
     
     
     
    //Use MBean Logic to check the number of stale Workflow Items 
    private int checkStaleItems()
    {
        try
        {
            //Create a MBeanServer class
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    
            ObjectName workflowMBean = getWorkflowMBean(server);
    
            //Get the number of stale workflowitems from AEM
            Object staleWorkflowCount = server.invoke(workflowMBean, "countStaleWorkflows", new Object[]{null}, new String[] {String.class.getName()});
    
            int mystaleCount = (Integer)staleWorkflowCount; 
                    
            //Return the number of stale items 
            return mystaleCount ; 
             
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        return -1; 
      }
          
          
       private static ObjectName getWorkflowMBean(MBeanServerConnection server)
       {
           try
           {
           Set<ObjectName> names = server.queryNames(new ObjectName("com.adobe.granite.workflow:type=Maintenance,*"), null);
             
           if (names.isEmpty()) {
                   return null;
           }
    
           return names.iterator().next();
           }
           catch(Exception e)
           {
               e.printStackTrace();
           }
           return null; 
  }
  
 
       private void sendMail(int count)
       {
           // Recipient's email ID needs to be mentioned.
           String to = "SET THE TO ADDRESS";
 
           // Sender's email ID needs to be mentioned
           String from = "SET THE FROM ADDRESS";
 
           // Assuming you are sending email from localhost
           String host = "SET SMTP MAIL SERVER";
 
           // Get system properties
           Properties properties = System.getProperties();
 
           // Setup mail server
           properties.setProperty("mail.smtp.host", host);
           properties.setProperty("mail.smtp.user", "Enter STMP USER"); // Set the SMTP Mail user
            
 
           // Get the default Session object.
          // javax.mail.Authenticator authenticator = new  javax.mail.PasswordAuthentication("username", "password");
           Session session = Session.getDefaultInstance(properties);
 
           try{
               // Create a default MimeMessage object.
               MimeMessage message = new MimeMessage(session);
 
               // Set From: header field of the header.
               message.setFrom(new InternetAddress(from));
 
               // Set To: header field of the header.
               message.addRecipient(Message.RecipientType.TO,
                       new InternetAddress(to));
 
               // Set Subject: header field
               message.setSubject("Stale AEM Workflow Items");
 
               // Now set the actual message
               message.setText("Please note that there are "+count +" stale AEM workflows");
 
               // Send message
               Transport.send(message);
               log.info("Stale mail notification message sent message successfully....");
           }catch (Exception ex) {
               ex.printStackTrace();
           }
       }
 
}