import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.BuiltinExchangeType;

public class RabbitMQInit {
    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(25672);
        factory.setUsername("admin");
        factory.setPassword("admin");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // Declare exchange
            channel.exchangeDeclare("crawler.direct", BuiltinExchangeType.DIRECT, true, false, null);
            System.out.println("Exchange 'crawler.direct' created");

            channel.exchangeDeclare("dead.letter.direct", BuiltinExchangeType.DIRECT, true, false, null);
            System.out.println("Exchange 'dead.letter.direct' created");

            // Declare queues
            String[] queues = {
                "crawler.raw.content.queue",
                "crawler.content.encoded.queue",
                "crawler.crawl.result.queue",
                "crawler.crawl.error.queue",
                "crawler.raw.content.dlq",
                "crawler.content.encoded.dlq"
            };

            for (String queue : queues) {
                channel.queueDeclare(queue, true, false, false, null);
                System.out.println("Queue '" + queue + "' created");
            }

            // Declare bindings
            channel.queueBind("crawler.raw.content.queue", "crawler.direct", "raw.content");
            channel.queueBind("crawler.content.encoded.queue", "crawler.direct", "content.encoded");
            channel.queueBind("crawler.crawl.result.queue", "crawler.direct", "crawl.result");
            channel.queueBind("crawler.crawl.error.queue", "crawler.direct", "crawl.error");
            channel.queueBind("crawler.raw.content.dlq", "dead.letter.direct", "raw.content.dlq");
            channel.queueBind("crawler.content.encoded.dlq", "dead.letter.direct", "content.encoded.dlq");

            System.out.println("All bindings created");
            System.out.println("RabbitMQ initialization complete!");
        }
    }
}
