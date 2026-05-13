package com.discussion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a Reply to a DiscussionThread.
 * A thread can have zero or more replies (0..*).
 */
@Entity
@Table(name = "replies")
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int replyId;

    @Column(nullable = false)
    private int threadId;

    @Column(nullable = false)
    private int authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean isAccepted = false;

    @Column(nullable = false)
    private int upvotes = 0;

    @Column(length = 100)
    private String authorName;

    @ElementCollection
    @CollectionTable(name = "reply_upvotes", joinColumns = @JoinColumn(name = "reply_id"))
    @Column(name = "user_id")
    private java.util.Set<Integer> upvotedUserIds = new java.util.HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = true)
    private Integer parentReplyId;

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------

    public Reply() {
        this.createdAt = LocalDateTime.now();
    }

    public Reply(int threadId, int authorId, String body) {
        this.threadId = threadId;
        this.authorId = authorId;
        this.body = body;
        this.isAccepted = false;
        this.upvotes = 0;
        this.createdAt = LocalDateTime.now();
    }

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------

    public int getReplyId() {
        return replyId;
    }

    public void setReplyId(int replyId) {
        this.replyId = replyId;
    }

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isAccepted() {
        return isAccepted;
    }

    public void setAccepted(boolean isAccepted) {
        this.isAccepted = isAccepted;
    }

    public int getUpvotes() {
        return upvotes;
    }

    public void setUpvotes(int upvotes) {
        this.upvotes = upvotes;
    }

    public java.util.Set<Integer> getUpvotedUserIds() {
        return upvotedUserIds;
    }

    public void setUpvotedUserIds(java.util.Set<Integer> upvotedUserIds) {
        this.upvotedUserIds = upvotedUserIds;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Integer getParentReplyId() {
        return parentReplyId;
    }

    public void setParentReplyId(Integer parentReplyId) {
        this.parentReplyId = parentReplyId;
    }

    // ----------------------------------------------------------------
    // toString
    // ----------------------------------------------------------------

    @Override
    public String toString() {
        return "Reply{" +
                "replyId=" + replyId +
                ", threadId=" + threadId +
                ", authorId=" + authorId +
                ", isAccepted=" + isAccepted +
                ", upvotes=" + upvotes +
                ", createdAt=" + createdAt +
                ", parentReplyId=" + parentReplyId +
                '}';
    }
}
