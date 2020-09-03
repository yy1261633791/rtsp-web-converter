package com.converter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * Websocket
 *
 * @author lizhiyong
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /***
     * 注册 Stomp的端点 addEndpoint：添加STOMP协议的端点。提供WebSocket或SockJS客户端访问的地址
     *
     * @param registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/message").setAllowedOrigins("*");
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        // 设置最大消息发送长度为10M
        registry.setMessageSizeLimit(1024 * 1024 * 10);
        registry.setSendBufferSizeLimit(1024 * 1024 * 10);
        registry.setSendTimeLimit(20 * 10000);
    }

    /**
     * 配置消息代理 启动Broker，消息的发送的地址符合配置的前缀来的消息才发送到这个broker
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");// 推送消息前缀
        registry.setApplicationDestinationPrefixes("/topic");// 应用请求前缀
        registry.setUserDestinationPrefix("/user");// 推送用户前缀
    }

}
