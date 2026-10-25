package com.eventhub.unit.security;

import com.eventhub.infrastructure.security.upload.FileUploadSecurityValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class FileUploadSecurityValidatorUnitTest {

    private FileUploadSecurityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new FileUploadSecurityValidator();
    }

    @Test
    @DisplayName("Arquivo nulo ou vazio deve ser rejeitado")
    void shouldRejectNullOrEmptyFile() {
        assertThat(validator.validate(null).isValid()).isFalse();

        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.csv", "text/csv", new byte[0]);
        var result = validator.validate(emptyFile);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("empty or missing");
    }

    @Test
    @DisplayName("Arquivo excedendo limite de 50MB deve ser rejeitado")
    void shouldRejectOversizedFile() {
        // Mock a file with size greater than 50MB
        MockMultipartFile oversized = new MockMultipartFile("file", "large.csv", "text/csv", new byte[10]) {
            @Override
            public long getSize() {
                return 51L * 1024 * 1024;
            }
        };

        var result = validator.validate(oversized);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("exceeds maximum size limit");
    }

    @Test
    @DisplayName("Tentativas de Path Traversal no nome do arquivo devem ser bloqueadas")
    void shouldRejectPathTraversalInFilename() {
        MockMultipartFile file1 = new MockMultipartFile("file", "../../passwords.csv", "text/csv", "data".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "folder/sub/data.json", "application/json", "{}".getBytes());
        MockMultipartFile file3 = new MockMultipartFile("file", "null\0byte.txt", "text/plain", "hello".getBytes());
        MockMultipartFile file4 = new MockMultipartFile("file", "win\\path\\data.csv", "text/csv", "hello".getBytes());

        assertThat(validator.validate(file1).isValid()).isFalse();
        assertThat(validator.validate(file2).isValid()).isFalse();
        assertThat(validator.validate(file3).isValid()).isFalse();
        assertThat(validator.validate(file4).isValid()).isFalse();
    }

    @Test
    @DisplayName("Arquivos sem extensão ou com ponto final devem ser rejeitados")
    void shouldRejectFilesWithoutValidExtension() {
        MockMultipartFile noExt = new MockMultipartFile("file", "noextension", "text/plain", "data".getBytes());
        MockMultipartFile dotEnd = new MockMultipartFile("file", "endingdot.", "text/plain", "data".getBytes());
        MockMultipartFile nullName = new MockMultipartFile("file", null, "text/plain", "data".getBytes());

        assertThat(validator.validate(noExt).isValid()).isFalse();
        assertThat(validator.validate(dotEnd).isValid()).isFalse();
        assertThat(validator.validate(nullName).isValid()).isFalse();
    }

    @Test
    @DisplayName("Extensões não permitidas (.exe, .sh, .py, .bat) devem ser rejeitadas")
    void shouldRejectForbiddenExtensions() {
        MockMultipartFile exeFile = new MockMultipartFile("file", "malware.exe", "application/octet-stream", "data".getBytes());
        MockMultipartFile shFile = new MockMultipartFile("file", "script.sh", "text/x-sh", "data".getBytes());

        var resExe = validator.validate(exeFile);
        assertThat(resExe.isValid()).isFalse();
        assertThat(resExe.errorMessage()).contains("not permitted");

        var resSh = validator.validate(shFile);
        assertThat(resSh.isValid()).isFalse();
    }

    @Test
    @DisplayName("Arquivos com extensões permitidas (.csv, .json, .parquet, .xml, .txt) devem ser aceitos se o cabeçalho for limpo")
    void shouldAcceptWhitelistedExtensions() {
        MockMultipartFile csv = new MockMultipartFile("file", "events_2026.csv", "text/csv", "id,type\n1,PRD".getBytes());
        MockMultipartFile json = new MockMultipartFile("file", "dataset.json", "application/json", "{\"status\":\"ok\"}".getBytes());
        MockMultipartFile txt = new MockMultipartFile("file", "notes.txt", "text/plain", "texto simples".getBytes());

        assertThat(validator.validate(csv).isValid()).isTrue();
        assertThat(validator.validate(json).isValid()).isTrue();
        assertThat(validator.validate(txt).isValid()).isTrue();
    }

    @Test
    @DisplayName("Magic bytes maliciosos (MZ, ELF, Shebang, Mach-O) devem ser rejeitados")
    void shouldRejectDisguisedBinariesViaMagicBytes() {
        byte[] mzHeader = new byte[]{(byte) 0x4D, (byte) 0x5A, 0x00, 0x00};
        MockMultipartFile disguisedExe = new MockMultipartFile("file", "fake.csv", "text/csv", mzHeader);
        var resMz = validator.validate(disguisedExe);
        assertThat(resMz.isValid()).isFalse();
        assertThat(resMz.errorMessage()).contains("Executable binary (DOS/PE) detected");

        byte[] elfHeader = new byte[]{(byte) 0x7F, (byte) 0x45, (byte) 0x4C, (byte) 0x46};
        MockMultipartFile disguisedElf = new MockMultipartFile("file", "fake.json", "application/json", elfHeader);
        var resElf = validator.validate(disguisedElf);
        assertThat(resElf.isValid()).isFalse();
        assertThat(resElf.errorMessage()).contains("Linux executable binary (ELF) detected");

        byte[] shellHeader = new byte[]{(byte) 0x23, (byte) 0x21, 0x2F, 0x62}; // #!/b
        MockMultipartFile disguisedShell = new MockMultipartFile("file", "fake.txt", "text/plain", shellHeader);
        var resShell = validator.validate(disguisedShell);
        assertThat(resShell.isValid()).isFalse();
        assertThat(resShell.errorMessage()).contains("Shell script detected");

        byte[] macho1 = new byte[]{(byte) 0xFE, (byte) 0xED, (byte) 0xFA, 0x00};
        MockMultipartFile disguisedMacho1 = new MockMultipartFile("file", "fake1.parquet", "application/octet-stream", macho1);
        var resMacho1 = validator.validate(disguisedMacho1);
        assertThat(resMacho1.isValid()).isFalse();
        assertThat(resMacho1.errorMessage()).contains("Mach-O executable binary detected");

        byte[] macho2 = new byte[]{(byte) 0xCF, (byte) 0xFA, (byte) 0xED, (byte) 0xFE};
        MockMultipartFile disguisedMacho2 = new MockMultipartFile("file", "fake2.xml", "application/xml", macho2);
        var resMacho2 = validator.validate(disguisedMacho2);
        assertThat(resMacho2.isValid()).isFalse();
        assertThat(resMacho2.errorMessage()).contains("Mach-O executable binary detected");
    }

    @Test
    @DisplayName("IOException na leitura do stream deve retornar erro de inspeção")
    void shouldHandleStreamReadException() {
        MockMultipartFile errFile = new MockMultipartFile("file", "valid.csv", "text/csv", "data".getBytes()) {
            @Override
            public InputStream getInputStream() throws IOException {
                throw new IOException("Simulated disk error");
            }
        };

        var result = validator.validate(errFile);
        assertThat(result.isValid()).isFalse();
        assertThat(result.errorMessage()).contains("Failed to inspect file stream");
    }
}
