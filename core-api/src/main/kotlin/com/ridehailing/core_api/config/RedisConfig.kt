package com.ridehailing.core_api.config

import com.ridehailing.core_api.ride.RideDispatchSubscriber
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter

@Configuration
open class RedisConfig {

  companion object {
    const val DISPATCH_CHANNEL = "ride:dispatch"
  }

  @Bean
  open fun dispatchTopic(): ChannelTopic = ChannelTopic(DISPATCH_CHANNEL)

  @Bean
  open fun messageListenerAdapter(subscriber: RideDispatchSubscriber): MessageListenerAdapter {
    return MessageListenerAdapter(subscriber, "onMessage")
  }

  @Bean
  open fun redisMessageListenerContainer(
    connectionFactory: RedisConnectionFactory,
    listenerAdapter: MessageListenerAdapter,
    topic: ChannelTopic
  ): RedisMessageListenerContainer {
    val container = RedisMessageListenerContainer()
    container.setConnectionFactory(connectionFactory)
    container.addMessageListener(listenerAdapter, topic)
    return container
  }
}
