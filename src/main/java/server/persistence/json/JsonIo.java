package server.persistence.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class JsonIo {
    static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    static synchronized <T> void saveAtomic(Path file, T obj) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName().toString() + ".tmp");
        Path bak = file.resolveSibling(file.getFileName().toString() + ".bak");
        byte[] bytes = MAPPER.writeValueAsBytes(obj);
        Files.write(tmp, bytes);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.copy(file, bak, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) {}
    }
}



