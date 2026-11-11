package com.kanban.backend.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** 디스크 저장 이름은 짧게(UUID+확장자), DB에 남기는 원래 이름은 칸 길이(255자)에 맞춘다. */
class FileNameTest {

    @Test
    void 확장자만_안전하게_떼어_낸다() {
        assertThat(LocalFileStorageService.safeExtension("회의록.PDF")).isEqualTo(".pdf");
        assertThat(LocalFileStorageService.safeExtension("사진.jpeg")).isEqualTo(".jpeg");
        assertThat(LocalFileStorageService.safeExtension("확장자없음")).isEmpty();
        assertThat(LocalFileStorageService.safeExtension("끝이점.")).isEmpty();
        assertThat(LocalFileStorageService.safeExtension("이상한.확장자")).isEmpty();
        assertThat(LocalFileStorageService.safeExtension("a.verylongextension")).isEmpty();
    }

    @Test
    void 긴_한글_이름도_저장_이름은_리눅스_한도_안이다() {
        String name = "가".repeat(200) + ".pdf";
        String key = java.util.UUID.randomUUID() + LocalFileStorageService.safeExtension(name);
        assertThat(key.getBytes(StandardCharsets.UTF_8).length).isLessThan(255);
    }

    @Test
    void 원래_이름이_255자를_넘으면_확장자는_살리고_줄인다() {
        String fitted = ProjectFileService.fitFileName("가".repeat(300) + ".docx");
        assertThat(fitted).hasSize(255).endsWith(".docx");
        assertThat(ProjectFileService.fitFileName("짧은이름.txt")).isEqualTo("짧은이름.txt");
    }
}
