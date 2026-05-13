package com.discussion.service;

import com.discussion.entity.DiscussionThread;
import com.discussion.entity.Reply;
import com.discussion.messaging.DiscussionNotificationPublisher;
import com.discussion.repository.ReplyRepository;
import com.discussion.repository.ThreadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * Implementation of DiscussionService.
 * Handles all business logic for threads, replies, moderation, and voting.
 */
@Service
@Transactional
public class DiscussionServiceImpl implements DiscussionService {

    private final ThreadRepository threadRepo;
    private final ReplyRepository replyRepo;
    private final DiscussionNotificationPublisher notificationPublisher;

    // ----------------------------------------------------------------
    // Constructor Injection
    // ----------------------------------------------------------------

    @Autowired
    public DiscussionServiceImpl(ThreadRepository threadRepo,
                                 ReplyRepository replyRepo,
                                 DiscussionNotificationPublisher notificationPublisher) {
        this.threadRepo = threadRepo;
        this.replyRepo = replyRepo;
        this.notificationPublisher = notificationPublisher;
    }

    // ----------------------------------------------------------------
    // Thread Operations
    // ----------------------------------------------------------------

    /**
     * Persist and return a new DiscussionThread.
     */
    @Override
    public DiscussionThread createThread(DiscussionThread thread) {
        if (thread.getTitle() == null || thread.getTitle().isBlank()) {
            throw new IllegalArgumentException("Thread title must not be blank.");
        }
        if (thread.getBody() == null || thread.getBody().isBlank()) {
            throw new IllegalArgumentException("Thread body must not be blank.");
        }
        DiscussionThread saved = threadRepo.save(thread);
        notificationPublisher.publishThreadCreated(saved);
        return saved;
    }

    /**
     * Get all threads for the specified course.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<DiscussionThread> getThreadsByCourse(int courseId, Pageable pageable) {
        return threadRepo.findByCourseId(courseId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscussionThread> getThreadsByCourses(List<Integer> courseIds, Pageable pageable) {
        return threadRepo.findByCourseIdIn(courseIds, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscussionThread> getThreadsByLesson(int lessonId, Pageable pageable) {
        return threadRepo.findByLessonId(lessonId, pageable);
    }

    /**
     * Toggle the pinned status of a thread.
     */
    @Override
    public void pinThread(int threadId) {
        DiscussionThread thread = findThreadOrThrow(threadId);
        thread.setPinned(!thread.isPinned());
        threadRepo.save(thread);
    }

    /**
     * Close a thread so no further replies can be added.
     */
    @Override
    public void closeThread(int threadId) {
        DiscussionThread thread = findThreadOrThrow(threadId);
        thread.setClosed(true);
        threadRepo.save(thread);
    }

    /**
     * Delete a thread and all associated replies.
     */
    @Override
    public void deleteThread(int threadId) {
        findThreadOrThrow(threadId); // validate existence
        replyRepo.deleteByThreadId(threadId);
        threadRepo.deleteById(threadId);
    }

    @Override
    public void upvoteThread(int threadId, int userId) {
        DiscussionThread thread = findThreadOrThrow(threadId);
        if (thread.getUpvotedUserIds().contains(userId)) {
            // Already upvoted, remove it (toggle behavior)
            thread.getUpvotedUserIds().remove(userId);
            thread.setUpvotes(Math.max(0, thread.getUpvotes() - 1));
        } else {
            thread.getUpvotedUserIds().add(userId);
            thread.setUpvotes(thread.getUpvotes() + 1);
        }
        threadRepo.save(thread);
    }

    // ----------------------------------------------------------------
    // Reply Operations
    // ----------------------------------------------------------------

    /**
     * Post a reply to an existing, open thread.
     */
    @Override
    public Reply postReply(int threadId, Reply reply) {
        DiscussionThread thread = findThreadOrThrow(threadId);
        if (thread.isClosed()) {
            throw new IllegalStateException("Cannot post a reply to a closed thread (threadId=" + threadId + ").");
        }
        reply.setThreadId(threadId);
        Reply saved = replyRepo.save(reply);
        
        // Increment replies count on thread
        thread.setRepliesCount(thread.getRepliesCount() + 1);
        threadRepo.save(thread);
        notificationPublisher.publishReplyCreated(thread, saved);
        
        return saved;
    }

    /**
     * Get all replies for the specified thread.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<Reply> getRepliesByThread(int threadId, Pageable pageable) {
        findThreadOrThrow(threadId); // validate thread exists
        return replyRepo.findByThreadId(threadId, pageable);
    }

    /**
     * Increment upvote count for a reply by 1.
     */
    @Override
    public void upvoteReply(int replyId, int userId) {
        Reply reply = findReplyOrThrow(replyId);
        if (reply.getUpvotedUserIds().contains(userId)) {
            // Toggle off
            reply.getUpvotedUserIds().remove(userId);
            reply.setUpvotes(Math.max(0, reply.getUpvotes() - 1));
        } else {
            reply.getUpvotedUserIds().add(userId);
            reply.setUpvotes(reply.getUpvotes() + 1);
        }
        replyRepo.save(reply);
    }

    /**
     * Mark a reply as the accepted/best answer.
     */
    @Override
    public void acceptReply(int replyId) {
        Reply reply = findReplyOrThrow(replyId);
        reply.setAccepted(true);
        replyRepo.save(reply);
    }

    /**
     * Delete a specific reply by ID.
     */
    @Override
    public void deleteReply(int replyId) {
        findReplyOrThrow(replyId); // validate existence
        // Delete sub-replies first (recursive simple one-level or loop)
        List<Reply> subReplies = replyRepo.findByParentReplyId(replyId);
        if (subReplies != null && !subReplies.isEmpty()) {
            replyRepo.deleteAll(subReplies);
        }
        replyRepo.deleteById(replyId);
    }

    // ----------------------------------------------------------------
    // Private Helpers
    // ----------------------------------------------------------------

    private DiscussionThread findThreadOrThrow(int threadId) {
        return threadRepo.findById(threadId)
                .orElseThrow(() -> new RuntimeException("Thread not found with id: " + threadId));
    }

    private Reply findReplyOrThrow(int replyId) {
        return replyRepo.findById(replyId)
                .orElseThrow(() -> new RuntimeException("Reply not found with id: " + replyId));
    }
}
