package com.duoc.gestionguiasdespacho.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.guias.exchange}")
    private String guiasExchange;

    @Value("${app.rabbitmq.guias.queue}")
    private String guiasQueue;

    @Value("${app.rabbitmq.guias.routing-key}")
    private String guiasRoutingKey;

    @Value("${app.rabbitmq.errores.exchange}")
    private String erroresExchange;

    @Value("${app.rabbitmq.errores.queue}")
    private String erroresQueue;

    @Value("${app.rabbitmq.errores.routing-key}")
    private String erroresRoutingKey;

    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(
                guiasExchange,
                true,
                false
        );
    }

    @Bean
    public DirectExchange erroresExchange() {
        return new DirectExchange(
                erroresExchange,
                true,
                false
        );
    }

    @Bean
    public Queue guiasQueue() {
        return QueueBuilder
                .durable(guiasQueue)
                .deadLetterExchange(erroresExchange)
                .deadLetterRoutingKey(erroresRoutingKey)
                .build();
    }

    @Bean
    public Queue erroresQueue() {
        return QueueBuilder
                .durable(erroresQueue)
                .build();
    }

    @Bean
    public Binding guiasBinding() {
        return BindingBuilder
                .bind(guiasQueue())
                .to(guiasExchange())
                .with(guiasRoutingKey);
    }

    @Bean
    public Binding erroresBinding() {
        return BindingBuilder
                .bind(erroresQueue())
                .to(erroresExchange())
                .with(erroresRoutingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
