package com.zjgsu.hwy.repair;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zjgsu.hwy.resident.repository.ResidentRepository;
import com.zjgsu.hwy.repair.repository.RepairTicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RepairWorkflowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    ResidentRepository residents;

    @Autowired
    RepairTicketRepository tickets;

    @BeforeEach
    void clean() {
        tickets.deleteAll();
        residents.deleteAll();
    }

    @Test
    void createResidentTicketAndQuery() throws Exception {
        String r = "{\"fullName\":\"张三\",\"phone\":\"13800000001\","
                + "\"building\":\"3号楼\",\"unitNumber\":\"2单元501\"}";

        String rr = mvc.perform(post("/api/residents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(r))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long rid = mapper.readTree(rr).get("id").asLong();
        assertThat(residents.findById(rid)).isPresent();

        String t = "{\"title\":\"楼道灯损坏\","
                + "\"description\":\"三楼楼道灯不亮\","
                + "\"location\":\"3号楼三层\","
                + "\"residentId\":" + rid + "}";

        String tr = mvc.perform(post("/api/repair-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(t))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long tid = mapper.readTree(tr).get("id").asLong();
        assertThat(tickets.findById(tid)).isPresent();

        mvc.perform(get("/api/repair-tickets/{id}", tid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("楼道灯损坏"));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mvc.perform(post("/api/repair-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\" \","
                                + "\"description\":\"灯坏了\","
                                + "\"location\":\"3号楼\","
                                + "\"residentId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void rejectsSkippingStatusSteps() throws Exception {
        String r = "{\"fullName\":\"李四\",\"phone\":\"13800000002\","
                + "\"building\":\"1号楼\",\"unitNumber\":\"1单元101\"}";

        String rr = mvc.perform(post("/api/residents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(r))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long rid = mapper.readTree(rr).get("id").asLong();

        String t = "{\"title\":\"门禁故障\","
                + "\"description\":\"门禁无法打开\","
                + "\"location\":\"东门\","
                + "\"residentId\":" + rid + "}";

        String tr = mvc.perform(post("/api/repair-tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(t))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long tid = mapper.readTree(tr).get("id").asLong();

        mvc.perform(patch("/api/repair-tickets/{id}/status", tid)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthIsUp() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}