package com.realtime.demo;

import javax.management.ObjectName;

import org.apache.activemq.broker.jmx.BrokerViewMBean;
import org.apache.activemq.web.RemoteJMXBrokerFacade;
import org.apache.activemq.web.config.SystemPropertiesConfiguration;
/** 
 *  
 * RemoteJMXBrokerFacade 访问ActiveMQ JMX配置 
 * @author   Jeremy Li    
 * 
 */
public class ActiveMQJMX {
    /** 
     *       通过JMX获取ActiveMQ各种信息 
     * @param args 
     */
    public static void main(String[] args) {
        RemoteJMXBrokerFacade createConnector = new RemoteJMXBrokerFacade();
        System.setProperty(
            "webconsole.jmx.url",
            "service:jmx:rmi:///jndi/rmi://localhost:1616/jmxrmi");
        System.setProperty("webconsole.jmx.user", "admin");
        System.setProperty("webconsole.jmx.password", "admin");
        SystemPropertiesConfiguration configuration = new SystemPropertiesConfiguration();
        createConnector.setConfiguration(configuration);
        try {
            BrokerViewMBean brokerAdmin = createConnector.getBrokerAdmin();
            String brokerName = brokerAdmin.getBrokerName();
            System.out.println("BrokerName =" + brokerName);
            long messages = brokerAdmin.getTotalMessageCount();
            System.out.println("messages =" + messages);
            long consumerCount = brokerAdmin.getTotalConsumerCount();
            System.out.println("consumerCount =" + consumerCount);
            long dequeueCount = brokerAdmin.getTotalDequeueCount();
            System.out.println("dequeueCount =" + dequeueCount);
            long enqueueCount = brokerAdmin.getTotalEnqueueCount();
            System.out.println("enqueueCount =" + enqueueCount);

            System.out.println(brokerAdmin.getBrokerName());
            //获取Topic相关的ObjectName  
            ObjectName[] topicList = brokerAdmin.getTopics();
            System.out.println("topic =" + topicList.length);
            //获取Queue相关的ObjectName  
            ObjectName[] queueList = brokerAdmin.getQueues();
            System.out.println("queue =" + queueList.length);
            //根据ObjectName创建相关的JMX对象获取相关的信息。  
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}