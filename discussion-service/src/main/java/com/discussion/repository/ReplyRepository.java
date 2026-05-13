package com.discussion.repository;

import com.discussion.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Integer> {

    Page<Reply> findByThreadId(int threadId, Pageable pageable);

    void deleteByThreadId(int threadId);

    /**
     * Find all replies by a specific author.
     *
     * @param authorId the author identifier
     * @return list of Replies by the given author
     */
    List<Reply> findByAuthorId(int authorId);

    /**
     * Find all sub-replies for a specific reply.
     *
     * @param parentReplyId the parent reply identifier
     * @return list of sub-replies
     */
    List<Reply> findByParentReplyId(int parentReplyId);
}
