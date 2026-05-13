package com.discussion.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a Discussion Thread in a course forum.
 * Threads are linked to a course and optionally to a specific lesson.
 */
@Entity
@Table(name = "discussion_threads")
public class DiscussionThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int threadId;

    @Column(nullable = false)
    private int courseId;

    private int lessonId;

    @Column(nullable = false)
    private int authorId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(nullable = false)
    private boolean isPinned = false;

    @Column(nullable = false)
    private boolean isClosed = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private int upvotes = 0;

    @Column(length = 100)
    private String authorName;

    private int repliesCount = 0;

    @ElementCollection
    @CollectionTable(name = "thread_upvotes", joinColumns = @JoinColumn(name = "thread_id"))
    @Column(name = "user_id")
    private java.util.Set<Integer> upvotedUserIds = new java.util.HashSet<>();

    // ----------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------

    public DiscussionThread() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public DiscussionThread(int courseId, int lessonId, int authorId, String title, String body) {
        this.courseId = courseId;
        this.lessonId = lessonId;
        this.authorId = authorId;
        this.title = title;
        this.body = body;
        this.isPinned = false;
        this.isClosed = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // ----------------------------------------------------------------
    // Lifecycle Callbacks
    // ----------------------------------------------------------------

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ----------------------------------------------------------------
    // Getters & Setters
    // ----------------------------------------------------------------

    public int getThreadId() {
        return threadId;
    }

    public void setThreadId(int threadId) {
        this.threadId = threadId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public int getLessonId() {
        return lessonId;
    }

    public void setLessonId(int lessonId) {
        this.lessonId = lessonId;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean isPinned) {
        this.isPinned = isPinned;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void setClosed(boolean isClosed) {
        this.isClosed = isClosed;
        this.updatedAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
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

    public int getRepliesCount() {
        return repliesCount;
    }

    public void setRepliesCount(int repliesCount) {
        this.repliesCount = repliesCount;
    }

    // ----------------------------------------------------------------
    // toString
    // ----------------------------------------------------------------

    @Override
    public String toString() {
        return "DiscussionThread{" +
                "threadId=" + threadId +
                ", courseId=" + courseId +
                ", lessonId=" + lessonId +
                ", authorId=" + authorId +
                ", title='" + title + '\'' +
                ", isPinned=" + isPinned +
                ", isClosed=" + isClosed +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
