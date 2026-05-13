package com.lms.assessment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.assessment.entity.Attempt;
import com.lms.assessment.entity.Question;
import com.lms.assessment.entity.Quiz;
import com.lms.assessment.service.AssessmentService;
import com.lms.assessment.config.JwtAuthenticationFilter;
import com.lms.assessment.util.JwtUtil;
import com.lms.assessment.resource.AssessmentResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssessmentResource.class)
@AutoConfigureMockMvc(addFilters = false)
class AssessmentResourceTest {

        private static final String MSG = "message";
        private static final String QUIZ_NOT_FOUND = "Quiz not found";

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private AssessmentService assessService;

        @MockBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;

        @MockBean
        private JwtUtil jwtUtil;

        @Autowired
        private ObjectMapper objectMapper;

        private Quiz mockQuiz;
        private Question mockQuestion;
        private Attempt mockAttempt;

        @BeforeEach
        void setUp() {
                mockQuiz = new Quiz();
                mockQuiz.setQuizId(1);
                mockQuiz.setTitle("Java Quiz");
                mockQuiz.setCourseId(101);
                mockQuiz.setPassingScore(60);
                mockQuiz.setMaxAttempts(3);

                mockQuestion = new Question();
                mockQuestion.setQuestionId(1);
                mockQuestion.setQuizId(1);
                mockQuestion.setText("What is Java?");
                mockQuestion.setCorrectAnswer("Language");
                mockQuestion.setMarks(10);
                mockQuestion.setType("MCQ");
                mockQuestion.setOrderIndex(1);

                mockAttempt = new Attempt(1, 10);
                mockAttempt.setAttemptId(500);
                mockAttempt.setScore(80);
                mockAttempt.setPassed(true);
        }

        @Test
        void createQuiz_returns201() throws Exception {
                when(assessService.createQuiz(any(Quiz.class))).thenReturn(mockQuiz);

                mockMvc.perform(post("/api/assessment/quizzes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mockQuiz)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.title").value("Java Quiz"));
        }

        @Test
        void createQuiz_badRequest() throws Exception {
                when(assessService.createQuiz(any())).thenThrow(new IllegalArgumentException("Missing title"));
                mockMvc.perform(post("/api/assessment/quizzes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void updateQuiz_returns200() throws Exception {
                when(assessService.updateQuiz(any(Quiz.class))).thenReturn(mockQuiz);

                mockMvc.perform(put("/api/assessment/quizzes")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mockQuiz)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.quizId").value(1));
        }

        @Test
        void deleteQuiz_returns204() throws Exception {
                doNothing().when(assessService).deleteQuiz(1);

                mockMvc.perform(delete("/api/assessment/quizzes/1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void publishQuiz_returns200() throws Exception {
                doNothing().when(assessService).publishQuiz(1);

                mockMvc.perform(put("/api/assessment/quizzes/1/publish"))
                                .andExpect(status().isOk());
        }

        @Test
        void addQuestion_returns201() throws Exception {
                when(assessService.addQuestion(eq(1), any(Question.class)))
                                .thenReturn(mockQuestion);

                mockMvc.perform(post("/api/assessment/quizzes/1/questions")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(mockQuestion)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.text").value("What is Java?"));
        }

        @Test
        void getQuestionsByQuiz_returns200() throws Exception {
                when(assessService.getQuestionsByQuiz(1)).thenReturn(List.of(mockQuestion));

                mockMvc.perform(get("/api/assessment/quizzes/1/questions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].text").value("What is Java?"));
        }

        @Test
        void getQuizById_returns200() throws Exception {
                when(assessService.getQuizById(1)).thenReturn(mockQuiz);

                mockMvc.perform(get("/api/assessment/quizzes/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.quizId").value(1));
        }

        @Test
        void getByCourse_returns200() throws Exception {
                when(assessService.getQuizzesByCourse(101)).thenReturn(List.of(mockQuiz));

                mockMvc.perform(get("/api/assessment/quizzes/course/101"))
                                .andExpect(status().isOk());
        }

        @Test
        void startAttempt_returns201() throws Exception {
                when(assessService.startAttempt(10, 1)).thenReturn(mockAttempt);

                mockMvc.perform(post("/api/assessment/attempts/start")
                                .param("studentId", "10")
                                .param("quizId", "1"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.attemptId").value(500));
        }

        @Test
        void submitAttempt_returns200() throws Exception {
                Map<Integer, String> answers = new HashMap<>();
                answers.put(1, "Language");
                when(assessService.submitAttempt(eq(500), anyMap()))
                                .thenReturn(mockAttempt);

                mockMvc.perform(post("/api/assessment/attempts/500/submit")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(answers)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.score").value(80));
        }

        @Test
        void getByStudent_returns200() throws Exception {
                when(assessService.getAttemptsByStudent(10)).thenReturn(List.of(mockAttempt));

                mockMvc.perform(get("/api/assessment/attempts/student/10"))
                                .andExpect(status().isOk());
        }

        @Test
        void getBestScore_returns200() throws Exception {
                when(assessService.getBestScore(10, 1)).thenReturn(85);

                mockMvc.perform(get("/api/assessment/attempts/best")
                                .param("studentId", "10")
                                .param("quizId", "1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").value(85));
        }
}