package com.lms.notification.service;

import com.lms.notification.dto.EmailRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsyncEmailConsumerTest {

    @Mock
    private MailService mailService;

    @Test
    void consumerMethods_delegateToMailService() {
        AsyncEmailConsumer consumer = new AsyncEmailConsumer(mailService);
        EmailRequest request = EmailRequest.builder().to("user@example.com").subject("Hi").build();

        consumer.consumeGeneralEmail(request);
        consumer.consumeAuthEmail(request);
        consumer.consumePurchaseEmail(request);
        consumer.consumeInstructorEmail(request);

        verify(mailService, times(4)).sendEmail(request);
    }

    @Test
    void consumerMethods_swallowFailures() {
        AsyncEmailConsumer consumer = new AsyncEmailConsumer(mailService);
        EmailRequest request = EmailRequest.builder().to("user@example.com").subject("Hi").build();
        doThrow(new RuntimeException("mail failed")).when(mailService).sendEmail(request);

        consumer.consumeGeneralEmail(request);

        verify(mailService).sendEmail(request);
    }
}
