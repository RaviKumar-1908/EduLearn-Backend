package com.lms.notification.service;

import com.lms.notification.dto.EmailRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncEmailProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    private AsyncEmailProducer producer;

    @BeforeEach
    void setUp() {
        producer = new AsyncEmailProducer(rabbitTemplate);
        ReflectionTestUtils.setField(producer, "exchange", "lms.events.exchange");
    }

    @Test
    void queueEmail_skipsNullOrBlankRecipient() {
        producer.queueEmail(null);
        producer.queueEmail(EmailRequest.builder().to(" ").build());

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void queueEmail_success() {
        EmailRequest request = EmailRequest.builder().to("user@example.com").subject("Hello").build();

        producer.queueEmail(request);

        verify(rabbitTemplate).convertAndSend("lms.events.exchange", "notification.email.general", request);
    }

    @Test
    void queueEmail_swallowsAmqpAndUnexpectedExceptions() {
        EmailRequest request = EmailRequest.builder().to("user@example.com").subject("Hello").build();
        doThrow(new AmqpException("rabbit down")).when(rabbitTemplate)
                .convertAndSend("lms.events.exchange", "notification.email.general", request);

        producer.queueEmail(request);

        reset(rabbitTemplate);
        doThrow(new RuntimeException("serialization failed")).when(rabbitTemplate)
                .convertAndSend("lms.events.exchange", "notification.email.general", request);

        producer.queueEmail(request);

        verify(rabbitTemplate, times(1)).convertAndSend("lms.events.exchange", "notification.email.general", request);
    }
}
