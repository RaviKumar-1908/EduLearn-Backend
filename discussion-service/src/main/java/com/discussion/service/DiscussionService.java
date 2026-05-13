package com.discussion.service;

import com.discussion.entity.DiscussionThread;
import com.discussion.entity.Reply;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface DiscussionService {

    DiscussionThread createThread(DiscussionThread thread);

    Page<DiscussionThread> getThreadsByCourse(int courseId, Pageable pageable);

    Page<DiscussionThread> getThreadsByCourses(List<Integer> courseIds, Pageable pageable);

    Page<DiscussionThread> getThreadsByLesson(int lessonId, Pageable pageable);

    /**
     * Pin or toggle pin on a thread (instructor/admin only).
     *
     * @param threadId the thread identifier
     */
    void pinThread(int threadId);

    /**
     * Close a thread so no new replies can be posted.
     *
     * @param threadId the thread identifier
     */
    void closeThread(int threadId);

    /**
     * Delete a thread and all its replies.
     *
     * @param threadId the thread identifier
     */
    void deleteThread(int threadId);

    /**
     * Upvote a thread by incrementing its upvote count.
     *
     * @param threadId the thread identifier
     */
    void upvoteThread(int threadId, int userId);

    // ----------------------------------------------------------------
    // Reply Operations
    // ----------------------------------------------------------------

    /**
     * Post a reply to an existing thread.
     *
     * @param threadId the thread to reply to
     * @param reply    the reply content
     * @return the persisted Reply
     */
    Reply postReply(int threadId, Reply reply);

    /**
     * Retrieve all replies for a given thread.
     *
     * @param threadId the thread identifier
     * @return Page of Replies
     */
    Page<Reply> getRepliesByThread(int threadId, Pageable pageable);

    /**
     * Upvote a reply by incrementing its upvote count.
     *
     * @param replyId the reply identifier
     */
    void upvoteReply(int replyId, int userId);

    /**
     * Mark a reply as the accepted/best answer (instructor/admin only).
     *
     * @param replyId the reply identifier
     */
    void acceptReply(int replyId);

    /**
     * Delete a specific reply.
     *
     * @param replyId the reply identifier
     */
    void deleteReply(int replyId);
}
