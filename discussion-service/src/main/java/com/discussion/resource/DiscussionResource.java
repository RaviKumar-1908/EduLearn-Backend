package com.discussion.resource;

import com.discussion.entity.DiscussionThread;
import com.discussion.entity.Reply;
import com.discussion.service.DiscussionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import java.util.List;

/**
 * REST Resource (Controller) for the Discussion/Forum Service.
 *
 * Base paths:
 *   /api/threads  – thread management
 *   /api/replies  – reply management
 *
 * Registered with Eureka as 'discussion-service' on port 8088.
 * Accessible via API Gateway at port 8000.
 */
@RestController
@RequestMapping("/api")
public class DiscussionResource {

    private static final Logger log = LoggerFactory.getLogger(DiscussionResource.class);
    private final DiscussionService discService;

    // ----------------------------------------------------------------
    // Constructor Injection
    // ----------------------------------------------------------------

    @Autowired
    public DiscussionResource(DiscussionService discService) {
        this.discService = discService;
    }

    // ================================================================
    // THREAD ENDPOINTS
    // ================================================================

    /**
     * POST /api/threads
     * Create a new discussion thread.
     *
     * @param thread the thread payload
     * @return 201 Created with the saved thread
     */
    @PostMapping("/threads")
    public ResponseEntity<DiscussionThread> createThread(@RequestBody DiscussionThread thread) {
        DiscussionThread created = discService.createThread(thread);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * GET /api/threads/courses?courseIds=1,2,3
     * Get all threads for multiple courses.
     */
    @RequestMapping(value = "/threads/courses", method = RequestMethod.GET)
    public ResponseEntity<Page<DiscussionThread>> getByCourses(
            @RequestParam("ids") List<Integer> ids,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("GET /api/threads/courses - ids={}, pageable={}", ids, pageable);
        Page<DiscussionThread> threads = discService.getThreadsByCourses(ids, pageable);
        return ResponseEntity.ok(threads);
    }

    @GetMapping("/threads/course/{courseId}")
    public ResponseEntity<Page<DiscussionThread>> getByCourse(
            @PathVariable int courseId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DiscussionThread> threads = discService.getThreadsByCourse(courseId, pageable);
        return ResponseEntity.ok(threads);
    }

    @GetMapping("/threads/lesson/{lessonId}")
    public ResponseEntity<Page<DiscussionThread>> getByLesson(
            @PathVariable int lessonId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<DiscussionThread> threads = discService.getThreadsByLesson(lessonId, pageable);
        return ResponseEntity.ok(threads);
    }

    /**
     * PUT /api/threads/{threadId}/pin
     * Toggle the pinned status of a thread (instructor/admin).
     *
     * @param threadId the thread identifier
     * @return 200 OK
     */
    @PutMapping("/threads/{threadId}/pin")
    public ResponseEntity<String> pinThread(@PathVariable int threadId) {
        discService.pinThread(threadId);
        return ResponseEntity.ok("Thread pin status toggled for threadId: " + threadId);
    }

    /**
     * PUT /api/threads/{threadId}/close
     * Close a thread (instructor/admin).
     *
     * @param threadId the thread identifier
     * @return 200 OK
     */
    @PutMapping("/threads/{threadId}/close")
    public ResponseEntity<String> closeThread(@PathVariable int threadId) {
        discService.closeThread(threadId);
        return ResponseEntity.ok("Thread closed for threadId: " + threadId);
    }

    /**
     * DELETE /api/threads/{threadId}
     * Delete a thread and all its replies (admin moderation).
     *
     * @param threadId the thread identifier
     * @return 204 No Content
     */
    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<Void> deleteThread(@PathVariable int threadId) {
        discService.deleteThread(threadId);
        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/threads/{threadId}/upvote
     * Upvote a thread.
     */
    @PutMapping("/threads/{threadId}/upvote")
    public ResponseEntity<String> upvoteThread(@PathVariable int threadId, @RequestParam int userId) {
        discService.upvoteThread(threadId, userId);
        return ResponseEntity.ok("Upvote status updated for threadId: " + threadId);
    }

    // ================================================================
    // REPLY ENDPOINTS
    // ================================================================

    /**
     * POST /api/threads/{threadId}/replies
     * Post a reply to a specific thread.
     *
     * @param threadId the thread to reply to
     * @param reply    the reply payload
     * @return 201 Created with the saved reply
     */
    @PostMapping("/threads/{threadId}/replies")
    public ResponseEntity<Reply> postReply(@PathVariable int threadId,
                                           @RequestBody Reply reply) {
        Reply created = discService.postReply(threadId, reply);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/threads/{threadId}/replies")
    public ResponseEntity<Page<Reply>> getReplies(
            @PathVariable int threadId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Reply> replies = discService.getRepliesByThread(threadId, pageable);
        return ResponseEntity.ok(replies);
    }

    /**
     * PUT /api/replies/{replyId}/upvote
     * Upvote a reply.
     *
     * @param replyId the reply identifier
     * @return 200 OK
     */
    @PutMapping("/replies/{replyId}/upvote")
    public ResponseEntity<String> upvoteReply(@PathVariable int replyId, @RequestParam int userId) {
        discService.upvoteReply(replyId, userId);
        return ResponseEntity.ok("Upvote status updated for replyId: " + replyId);
    }

    /**
     * PUT /api/replies/{replyId}/accept
     * Mark a reply as the accepted/best answer (instructor/admin).
     *
     * @param replyId the reply identifier
     * @return 200 OK
     */
    @PutMapping("/replies/{replyId}/accept")
    public ResponseEntity<String> acceptReply(@PathVariable int replyId) {
        discService.acceptReply(replyId);
        return ResponseEntity.ok("Reply accepted with replyId: " + replyId);
    }

    /**
     * DELETE /api/replies/{replyId}
     * Delete a reply (admin moderation).
     *
     * @param replyId the reply identifier
     * @return 204 No Content
     */
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(@PathVariable int replyId) {
        discService.deleteReply(replyId);
        return ResponseEntity.noContent().build();
    }
}
