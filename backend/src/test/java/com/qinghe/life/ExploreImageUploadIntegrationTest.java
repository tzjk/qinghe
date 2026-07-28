package com.qinghe.life;

import com.qinghe.life.oss.AliyunOSSOperator;
import java.io.InputStream;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ExploreImageUploadIntegrationTest extends ExploreTestSupport {
    @MockBean private AliyunOSSOperator ossOperator;

    @BeforeEach
    void mockOssUpload() {
        when(ossOperator.upload(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> "https://oss.example/" + invocation.getArgument(0));
    }

    @Test
    void rejectsUnauthenticatedAndInvalidExploreImages() throws Exception {
        mvc.perform(multipart("/api/explore/images").file(pngFile()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));
        mvc.perform(multipart("/api/explore/images").file(new MockMultipartFile("file", "not-image.txt", "text/plain", "text".getBytes()))
                        .header("Authorization", userAuthorization(USER_ONE_TOKEN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
        mvc.perform(multipart("/api/explore/images").file(new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]))
                        .header("Authorization", userAuthorization(USER_ONE_TOKEN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void uploadsValidatedImageToExplorePrefixAndReturnsOssUrl() throws Exception {
        mvc.perform(multipart("/api/explore/images").file(pngFile()).header("Authorization", userAuthorization(USER_ONE_TOKEN)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.containsString("https://oss.example/qinghe-life-service/explore/")));
    }

    private MockMultipartFile pngFile() {
        return new MockMultipartFile("file", "explore.png", "image/png", Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScL3vgAAAABJRU5ErkJggg=="));
    }
}
