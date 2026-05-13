package com.discussion.service;

import com.discussion.entity.DiscussionThread;
import com.discussion.entity.Reply;
import com.discussion.messaging.DiscussionNotificationPublisher;
import com.discussion.repository.ReplyRepository;
import com.discussion.repository.ThreadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscussionServiceImplTest {

    @Mock
    private ThreadRepository threadRepo;

    @Mock
    private ReplyRepository replyRepo;

    @Mock
    private DiscussionNotificationPublisher notificationPublisher;

    @InjectMocks
    private DiscussionServiceImpl discussionService;

    private DiscussionThread mockThread;
    private Reply mockReply;

    @BeforeEach
    void setUp() {
        mockThread = new DiscussionThread();
        mockThread.setThreadId(1);
        mockThread.setTitle("Help with Java");
        mockThread.setBody("I need help");
        mockThread.setCourseId(101);
        mockThread.setClosed(false);
        mockThread.setUpvotedUserIds(new HashSet<>());

        mockReply = new Reply();
        mockReply.setReplyId(10);
        mockReply.setThreadId(1);
        mockReply.setBody("I can help");
        mockReply.setUpvotedUserIds(new HashSet<>());
    }

    @Test
    void createThread_success() {
        when(threadRepo.save(any(DiscussionThread.class))).thenReturn(mockThread);
        
        DiscussionThread result = discussionService.createThread(mockThread);

        assertNotNull(result);
        assertEquals("Help with Java", result.getTitle());
        verify(threadRepo, times(1)).save(mockThread);
        verify(notificationPublisher).publishThreadCreated(mockThread);
    }

    @Test
    void postReply_success() {
        when(threadRepo.findById(1)).thenReturn(Optional.of(mockThread));
        when(replyRepo.save(any(Reply.class))).thenReturn(mockReply);
        when(threadRepo.save(any(DiscussionThread.class))).thenReturn(mockThread);

        Reply result = discussionService.postReply(1, mockReply);

        assertNotNull(result);
        assertEquals(1, mockThread.getRepliesCount());
        verify(replyRepo, times(1)).save(mockReply);
        verify(notificationPublisher).publishReplyCreated(mockThread, mockReply);
    }

    @Test
    void postReply_closedThread_throwsException() {
        mockThread.setClosed(true);
        when(threadRepo.findById(1)).thenReturn(Optional.of(mockThread));

        assertThrows(IllegalStateException.class, () -> discussionService.postReply(1, mockReply));
    }

    @Test
    void upvoteThread_success() {
        when(threadRepo.findById(1)).thenReturn(Optional.of(mockThread));
        when(threadRepo.save(any(DiscussionThread.class))).thenReturn(mockThread);

        discussionService.upvoteThread(1, 55);

        assertEquals(1, mockThread.getUpvotes());
        assertTrue(mockThread.getUpvotedUserIds().contains(55));
    }

    @Test
    void deleteThread_success() {
        when(threadRepo.findById(1)).thenReturn(Optional.of(mockThread));
        doNothing().when(replyRepo).deleteByThreadId(1);
        doNothing().when(threadRepo).deleteById(1);

        discussionService.deleteThread(1);

        verify(replyRepo, times(1)).deleteByThreadId(1);
        verify(threadRepo, times(1)).deleteById(1);
    }
}
