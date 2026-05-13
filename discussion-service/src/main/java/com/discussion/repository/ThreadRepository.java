package com.discussion.repository;

import com.discussion.entity.DiscussionThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ThreadRepository extends JpaRepository<DiscussionThread, Integer> {

    Page<DiscussionThread> findByCourseId(int courseId, Pageable pageable);

    Page<DiscussionThread> findByCourseIdIn(List<Integer> courseIds, Pageable pageable);

    Page<DiscussionThread> findByLessonId(int lessonId, Pageable pageable);

    List<DiscussionThread> findByAuthorId(int authorId);

    /**
     * Find all threads by their pinned status.
     *
     * @param isPinned true to get pinned threads, false for unpinned
     * @return list of DiscussionThreads matching the pinned status
     */
    List<DiscussionThread> findByIsPinned(boolean isPinned);

    /**
     * Search threads by keyword in title or body (case-insensitive).
     *
     * @param keyword the search keyword
     * @return list of DiscussionThreads matching the keyword
     */
    @Query("SELECT t FROM DiscussionThread t WHERE " +
           "LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(t.body)  LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<DiscussionThread> searchByKeyword(@Param("keyword") String keyword);
}
