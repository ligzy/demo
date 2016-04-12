package com.realtime.demo.amq;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

public class Sender {

    private static final int TEST_NUMBER = 10000;
    public static final String uri =
        //        "failover:(tcp://192.168.1.164:51616,tcp://192.168.1.249:51616,tcp://192.168.1.169:51616)?maxReconnectDelay=2000";
        "failover:(tcp://192.168.1.164:31616,tcp://192.168.1.249:31616,tcp://192.168.1.169:31616)?maxReconnectDelay=2000";

    public static void main(String[] args) {

        // ConnectionFactory：连接工厂，JMS用它创建连接

        ConnectionFactory connectionFactory;

        // Connection：JMS客户端到JMS Provider的连接

        Connection connection = null;

        // Session：一个发送或接收消息的线程

        Session session;

        // Destination：消息的目的地;消息发送给谁.

        Destination destination;

        // MessageProducer：消息发送者

        MessageProducer producer;

        // TextMessage message;

        //构造ConnectionFactory实例对象，此处采用ActiveMq的实现jar

        connectionFactory = new ActiveMQConnectionFactory(

            ActiveMQConnection.DEFAULT_USER,

            ActiveMQConnection.DEFAULT_PASSWORD,

            uri);

        try {

            //构造从工厂得到连接对象

            connection = connectionFactory.createConnection();

            //启动

            connection.start();

            //获取操作连接

            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            //获取session

            destination = session.createTopic("FirstQueue");

            //得到消息生成者【发送者】

            producer = session.createProducer(destination);

            //设置不持久化，此处学习，实际根据项目决定

            //            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            //构造消息，此处写死，项目就是参数，或者方法获取

            sendMessage(session, producer);

            session.commit();

        } catch (Exception e) {

            e.printStackTrace();

        } finally {

            try {

                if (null != connection)

                    connection.close();

            } catch (Throwable ignore) {

            }

        }

    }

    public static void sendMessage(Session session, MessageProducer producer) throws Exception {

        for (int i = 1; i <= TEST_NUMBER; i++) {

            String msg = "test activeMq message hello:" + i;
            TextMessage message = session.createTextMessage(msg);

            producer.send(message);

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            //发送消息到目的地方

            System.out.println("sent：" + msg);

        }

    }

}
