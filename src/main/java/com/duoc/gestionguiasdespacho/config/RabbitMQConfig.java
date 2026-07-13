package com.duoc.gestionguiasdespacho.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Configuration
public class RabbitMQConfig {

    private static final String PAQUETE_DTO_CONFIABLE =
            "com.duoc.gestionguiasdespacho.dto";

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
    public RabbitAdmin rabbitAdmin(
            ConnectionFactory connectionFactory
    ) {
        ConnectionFactory connectionFactorySegura =
                Objects.requireNonNull(
                        connectionFactory,
                        "ConnectionFactory no puede ser null."
                );

        RabbitAdmin rabbitAdmin =
                new RabbitAdmin(
                        connectionFactorySegura
                );

        rabbitAdmin.setAutoStartup(true);

        return rabbitAdmin;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        DefaultJackson2JavaTypeMapper typeMapper =
                new DefaultJackson2JavaTypeMapper();

        typeMapper.setTrustedPackages(
                PAQUETE_DTO_CONFIABLE
        );

        Jackson2JsonMessageConverter converter =
                new Jackson2JsonMessageConverter();

        converter.setJavaTypeMapper(
                typeMapper
        );

        return converter;
    }
}
