package com.discussion.resource;

import com.discussion.entity.DiscussionThread;
import com.discussion.entity.Reply;
import com.discussion.service.DiscussionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DiscussionResource.class)
@AutoConfigureMockMvc(addFilters = false)
class DiscussionResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DiscussionService discussionService;

    private DiscussionThread thread;
    private Reply reply;

    @BeforeEach
    void setUp() {
        thread = new DiscussionThread();
        thread.setThreadId(1);
        thread.setTitle("Java Help");
        thread.setCourseId(101);

        reply = new Reply();
        reply.setReplyId(1);
        reply.setBody("I can help!");
    }

    @Test
    void createThread_success() throws Exception {
        when(discussionService.createThread(any())).thenReturn(thread);

        mockMvc.perform(post("/api/threads")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(thread)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.threadId").value(1));
    }

    @Test
    void getByCourses_success() throws Exception {
        when(discussionService.getThreadsByCourses(anyList(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(thread)));

        mockMvc.perform(get("/api/threads/courses").param("ids", "1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getByCourse_success() throws Exception {
        when(discussionService.getThreadsByCourse(eq(101), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(thread)));

        mockMvc.perform(get("/api/threads/course/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getByLesson_success() throws Exception {
        when(discussionService.getThreadsByLesson(eq(201), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(thread)));

        mockMvc.perform(get("/api/threads/lesson/201"))
                .andExpect(status().isOk());
    }

    @Test
    void pinThread_success() throws Exception {
        doNothing().when(discussionService).pinThread(1);

        mockMvc.perform(put("/api/threads/1/pin"))
                .andExpect(status().isOk());
    }

    @Test
    void closeThread_success() throws Exception {
        doNothing().when(discussionService).closeThread(1);

        mockMvc.perform(put("/api/threads/1/close"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteThread_success() throws Exception {
        doNothing().when(discussionService).deleteThread(1);

        mockMvc.perform(delete("/api/threads/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void upvoteThread_success() throws Exception {
        doNothing().when(discussionService).upvoteThread(1, 10);

        mockMvc.perform(put("/api/threads/1/upvote").param("userId", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void postReply_success() throws Exception {
        when(discussionService.postReply(eq(1), any())).thenReturn(reply);

        mockMvc.perform(post("/api/threads/1/replies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reply)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.replyId").value(1));
    }

    @Test
    void getReplies_success() throws Exception {
        when(discussionService.getRepliesByThread(eq(1), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(reply)));

        mockMvc.perform(get("/api/threads/1/replies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void upvoteReply_success() throws Exception {
        doNothing().when(discussionService).upvoteReply(1, 10);

        mockMvc.perform(put("/api/replies/1/upvote").param("userId", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void acceptReply_success() throws Exception {
        doNothing().when(discussionService).acceptReply(1);

        mockMvc.perform(put("/api/replies/1/accept"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteReply_success() throws Exception {
        doNothing().when(discussionService).deleteReply(1);

        mockMvc.perform(delete("/api/replies/1"))
                .andExpect(status().isNoContent());
    }
}
