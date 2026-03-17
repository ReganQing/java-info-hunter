#!/usr/bin/env python3
import pika

credentials = pika.PlainCredentials('admin', 'admin')
parameters = pika.ConnectionParameters(host='localhost', port=25672, credentials=credentials)

try:
    connection = pika.BlockingConnection(parameters)
    channel = connection.channel()

    # Declare exchanges
    channel.exchange_declare('crawler.direct', exchange_type='direct', durable=True)
    print("Exchange 'crawler.direct' created")

    channel.exchange_declare('dead.letter.direct', exchange_type='direct', durable=True)
    print("Exchange 'dead.letter.direct' created")

    # Declare queues
    queues = [
        'crawler.raw.content.queue',
        'crawler.content.encoded.queue',
        'crawler.crawl.result.queue',
        'crawler.crawl.error.queue',
        'crawler.raw.content.dlq',
        'crawler.content.encoded.dlq'
    ]

    for queue in queues:
        channel.queue_declare(queue=queue, durable=True)
        print(f"Queue '{queue}' created")

    # Declare bindings
    bindings = [
        ('crawler.raw.content.queue', 'crawler.direct', 'raw.content'),
        ('crawler.content.encoded.queue', 'crawler.direct', 'content.encoded'),
        ('crawler.crawl.result.queue', 'crawler.direct', 'crawl.result'),
        ('crawler.crawl.error.queue', 'crawler.direct', 'crawl.error'),
        ('crawler.raw.content.dlq', 'dead.letter.direct', 'raw.content.dlq'),
        ('crawler.content.encoded.dlq', 'dead.letter.direct', 'content.encoded.dlq')
    ]

    for queue, exchange, routing_key in bindings:
        channel.queue_bind(queue=queue, exchange=exchange, routing_key=routing_key)
        print(f"Binding: {queue} -> {exchange} ({routing_key})")

    connection.close()
    print("RabbitMQ initialization complete!")

except Exception as e:
    print(f"Error: {e}")
