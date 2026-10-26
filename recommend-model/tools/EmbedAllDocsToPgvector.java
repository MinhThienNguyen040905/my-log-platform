import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Imports the prepared recommend-model corpus and embeds its missing chunks. */
public final class EmbedAllDocsToPgvector {
    private static final int DIMENSIONS = 768;
    private static final int BATCH_SIZE = 32;
    private static final String PROVIDER = "GEMINI";
    private static final String TOPIC = "RECOMMEND_MODEL_DOCS";
    private static final String LOCALE = "vi-VN";
    private static final SecureRandom RANDOM = new SecureRandom();

    private record Chunk(String source, String metadata, String text, UUID id) {}
    private record Pending(UUID id, String source, String text) {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Usage: EmbedAllDocsToPgvector ENV_FILE CHUNKS_TSV");
        Map<String, String> env = readEnv(Path.of(args[0]));
        String model = required(env, "MYLOG_RAG_EMBEDDING_MODEL");
        String modelVersion = required(env, "MYLOG_RAG_EMBEDDING_MODEL_VERSION");
        String apiKey = required(env, "MYLOG_RAG_EMBEDDING_API_KEY");
        if (!"true".equalsIgnoreCase(env.get("MYLOG_RAG_EMBEDDING_ENABLED"))
                || !"true".equalsIgnoreCase(env.get("MYLOG_RAG_EMBEDDING_PROVIDER_DATA_APPROVED")))
            throw new IllegalStateException("Embedding is disabled or provider data approval is missing");
        if (!"gemini".equals(env.get("MYLOG_RAG_EMBEDDING_PROVIDER"))
                || Integer.parseInt(env.getOrDefault("MYLOG_RAG_EMBEDDING_DIMENSIONS", "768")) != DIMENSIONS)
            throw new IllegalStateException("This importer expects the configured Gemini 768-dimensional model");

        List<Chunk> chunks = readChunks(Path.of(args[1]));
        Map<String, List<Chunk>> bySource = new LinkedHashMap<>();
        for (Chunk chunk : chunks) bySource.computeIfAbsent(chunk.source(), ignored -> new ArrayList<>()).add(chunk);
        int inserted = 0;
        try (Connection db = DriverManager.getConnection(required(env, "MYLOG_DB_URL"),
                required(env, "MYLOG_DB_USERNAME"), required(env, "MYLOG_DB_PASSWORD"))) {
            db.setAutoCommit(false);
            for (var entry : bySource.entrySet()) inserted += importSource(db, entry.getKey(), entry.getValue());
            db.commit();
            System.out.println("source_files=" + bySource.size() + " chunks_imported=" + inserted);

            List<Pending> pending = loadPending(db, model, modelVersion);
            System.out.println("chunks_waiting_for_embedding=" + pending.size());
            var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
            String endpoint = env.getOrDefault("MYLOG_RAG_EMBEDDING_ENDPOINT",
                    "https://generativelanguage.googleapis.com/v1beta").replaceAll("/+$", "")
                    + "/models/" + model + ":batchEmbedContents";
            for (int offset = 0; offset < pending.size(); offset += BATCH_SIZE) {
                List<Pending> batch = pending.subList(offset, Math.min(offset + BATCH_SIZE, pending.size()));
                List<float[]> vectors = embed(client, URI.create(endpoint), apiKey, model, batch);
                saveVectors(db, model, modelVersion, batch, vectors);
                db.commit();
                System.out.println("embedded=" + Math.min(offset + batch.size(), pending.size()) + "/" + pending.size());
            }
        }
    }

    private static int importSource(Connection db, String source, List<Chunk> chunks) throws Exception {
        String slug = "recommend-model-" + source.toLowerCase().replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
        UUID itemId;
        try (PreparedStatement find = db.prepareStatement("select id from knowledge_items where slug=?")) {
            find.setString(1, slug);
            try (ResultSet rs = find.executeQuery()) {
                if (rs.next()) return 0;
            }
        }
        itemId = uuid7();
        UUID versionId = uuid7();
        Instant now = Instant.now();
        StringBuilder content = new StringBuilder();
        for (Chunk chunk : chunks) content.append(chunk.text()).append('\n');
        byte[] checksum = MessageDigest.getInstance("SHA-256").digest(content.toString().getBytes(StandardCharsets.UTF_8));

        try (PreparedStatement insert = db.prepareStatement("""
                insert into knowledge_items(id,slug,topic_code,locale,source_name,source_url,owner_team,status,created_at,updated_at)
                values (?,?,?,?,?,null,'recommend-model','ACTIVE',?,?)
                """)) {
            insert.setObject(1, itemId); insert.setString(2, slug); insert.setString(3, TOPIC);
            insert.setString(4, LOCALE); insert.setString(5, "recommend-model/docs/" + source);
            insert.setTimestamp(6, Timestamp.from(now)); insert.setTimestamp(7, Timestamp.from(now)); insert.executeUpdate();
        }
        try (PreparedStatement insert = db.prepareStatement("""
                insert into knowledge_versions(id,item_id,version,title,content,content_sha256,status,review_notes,
                    chunk_strategy_version,created_by,reviewed_by,approved_by,approved_at,effective_from,effective_to,created_at,updated_at)
                values (?,?,1,?,?,?,'DRAFT','Bulk corpus import requested by the user; source review_status retained as chunk metadata.',
                    'docs-char1200-v1',null,null,null,null,null,null,?,?)
                """)) {
            insert.setObject(1, versionId); insert.setObject(2, itemId); insert.setString(3, source);
            insert.setString(4, content.toString()); insert.setBytes(5, checksum);
            insert.setTimestamp(6, Timestamp.from(now)); insert.setTimestamp(7, Timestamp.from(now)); insert.executeUpdate();
        }
        try (PreparedStatement insert = db.prepareStatement("""
                insert into knowledge_chunks(id,knowledge_version_id,chunk_index,content,token_count,metadata,created_at)
                values (?,?,?,?,?,cast(? as jsonb),?)
                """)) {
            for (int index = 0; index < chunks.size(); index++) {
                Chunk chunk = chunks.get(index);
                insert.setObject(1, chunk.id()); insert.setObject(2, versionId); insert.setInt(3, index);
                insert.setString(4, chunk.text()); insert.setInt(5, Math.max(1, chunk.text().trim().split("\\s+").length));
                insert.setString(6, chunk.metadata()); insert.setTimestamp(7, Timestamp.from(now)); insert.addBatch();
            }
            insert.executeBatch();
        }
        try (PreparedStatement audit = db.prepareStatement("""
                insert into audit_logs(id,actor_user_id,actor_type,action,target_type,target_id,reason_code,safe_metadata,occurred_at)
                values (?,null,'SYSTEM','KNOWLEDGE_DOCS_BULK_IMPORTED','KNOWLEDGE_ITEM',?,'USER_REQUEST',cast(? as jsonb),?)
                """)) {
            audit.setObject(1, uuid7()); audit.setObject(2, itemId);
            audit.setString(3, "{\"sourceFile\":\"" + jsonEscape(source) + "\",\"chunkCount\":" + chunks.size() + "}");
            audit.setTimestamp(4, Timestamp.from(now)); audit.executeUpdate();
        }
        return chunks.size();
    }

    private static List<Pending> loadPending(Connection db, String model, String modelVersion) throws Exception {
        var pending = new ArrayList<Pending>();
        try (PreparedStatement query = db.prepareStatement("""
                select c.id,i.source_name,c.content
                from knowledge_chunks c join knowledge_versions v on v.id=c.knowledge_version_id
                join knowledge_items i on i.id=v.item_id
                where i.slug like 'recommend-model-%' and i.status='ACTIVE' and v.status in ('DRAFT','APPROVED')
                  and not exists(select 1 from knowledge_embeddings e where e.chunk_id=c.id and e.provider=?
                    and e.model=? and e.model_version=? and e.dimensions=?)
                order by i.slug,c.chunk_index
                """)) {
            query.setString(1, PROVIDER); query.setString(2, model); query.setString(3, modelVersion);
            query.setInt(4, DIMENSIONS);
            try (ResultSet rs = query.executeQuery()) {
                while (rs.next()) pending.add(new Pending(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)));
            }
        }
        return pending;
    }

    private static List<float[]> embed(HttpClient client, URI endpoint, String apiKey,
                                       String model, List<Pending> batch) throws Exception {
        StringBuilder body = new StringBuilder("{\"requests\":[");
        for (int i = 0; i < batch.size(); i++) {
            if (i > 0) body.append(',');
            Pending chunk = batch.get(i);
            body.append("{\"model\":\"models/").append(jsonEscape(model))
                    .append("\",\"content\":{\"parts\":[{\"text\":\"").append(jsonEscape(chunk.text()))
                    .append("\"}]},\"taskType\":\"RETRIEVAL_DOCUMENT\",\"outputDimensionality\":")
                    .append(DIMENSIONS).append(",\"title\":\"").append(jsonEscape(chunk.source())).append("\"}");
        }
        body.append("]}");
        String lastFailure = "unknown provider failure";
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(45))
                        .header("Content-Type", "application/json").header("x-goog-api-key", apiKey)
                        .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 429 || response.statusCode() >= 500) {
                    lastFailure = "HTTP " + response.statusCode() + ": " + providerErrorMessage(response.body())
                            + response.headers().firstValue("Retry-After").map(value -> ", Retry-After=" + value).orElse("");
                    long retryAfter = response.headers().firstValue("Retry-After")
                            .map(value -> { try { return Long.parseLong(value) * 1000; } catch (NumberFormatException ignored) { return 0L; } })
                            .orElse(0L);
                    Thread.sleep(Math.max(retryAfter, Math.min(60_000, (1L << attempt) * 5000)));
                    continue;
                }
                if (response.statusCode() != 200) throw new IllegalStateException("Embedding provider returned HTTP " + response.statusCode());
                return parseVectors(response.body(), batch.size());
            } catch (IOException exception) {
                lastFailure = "network " + exception.getClass().getSimpleName();
                if (attempt == 4) throw new IllegalStateException("Embedding provider is unreachable: " + lastFailure);
                Thread.sleep(Math.min(30_000, (1L << attempt) * 1000));
            }
        }
        throw new IllegalStateException("Embedding provider retries exhausted: " + lastFailure);
    }

    private static String providerErrorMessage(String body) {
        Matcher matcher = Pattern.compile("\\\"message\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"").matcher(body);
        if (!matcher.find()) return "provider retryable error";
        return matcher.group(1).replace("\\n", " ").replace("\\\"", "\"");
    }

    private static List<float[]> parseVectors(String body, int expected) {
        Matcher matcher = Pattern.compile("\\\"values\\\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(body);
        var vectors = new ArrayList<float[]>(expected);
        while (matcher.find()) {
            String[] values = matcher.group(1).split(",");
            if (values.length != DIMENSIONS) throw new IllegalStateException(
                    "Embedding provider returned " + values.length + " dimensions; expected " + DIMENSIONS);
            float[] vector = new float[DIMENSIONS]; double norm = 0;
            for (int i = 0; i < values.length; i++) {
                double value = Double.parseDouble(values[i].trim());
                if (!Double.isFinite(value)) throw new IllegalStateException("Embedding provider returned a non-finite value");
                vector[i] = (float) value; norm += value * value;
            }
            if (norm == 0) throw new IllegalStateException("Embedding provider returned an empty vector");
            norm = Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) vector[i] = (float) (vector[i] / norm);
            vectors.add(vector);
        }
        if (vectors.size() != expected) throw new IllegalStateException("Embedding provider returned an invalid batch size");
        return vectors;
    }

    private static void saveVectors(Connection db, String model, String modelVersion,
                                    List<Pending> batch, List<float[]> vectors) throws Exception {
        try (PreparedStatement insert = db.prepareStatement("""
                insert into knowledge_embeddings(id,chunk_id,provider,model,model_version,dimensions,embedding,created_at)
                values (?,?,?,?,?,?,cast(? as vector),?) on conflict(chunk_id,provider,model,model_version) do nothing
                """)) {
            for (int i = 0; i < batch.size(); i++) {
                insert.setObject(1, uuid7()); insert.setObject(2, batch.get(i).id()); insert.setString(3, PROVIDER);
                insert.setString(4, model); insert.setString(5, modelVersion); insert.setInt(6, DIMENSIONS);
                insert.setString(7, vectorLiteral(vectors.get(i))); insert.setTimestamp(8, Timestamp.from(Instant.now())); insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private static List<Chunk> readChunks(Path file) throws Exception {
        var chunks = new ArrayList<Chunk>();
        for (String line : Files.readAllLines(file, StandardCharsets.US_ASCII)) {
            String[] fields = line.split("\\t", -1);
            if (fields.length != 4) throw new IllegalArgumentException("Malformed chunk feed");
            String chunkHash = decode(fields[0]);
            String source = decode(fields[1]);
            String metadata = decode(fields[2]);
            String text = decode(fields[3]);
            chunks.add(new Chunk(source, metadata, text, uuidFromHash(chunkHash)));
        }
        return chunks;
    }

    private static String decode(String value) { return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8); }

    private static UUID uuidFromHash(String hex) {
        String value = hex.substring(0, 32);
        value = value.substring(0, 12) + "5" + value.substring(13, 16)
                + "8" + value.substring(17);
        return UUID.fromString(value.substring(0, 8) + "-" + value.substring(8, 12) + "-"
                + value.substring(12, 16) + "-" + value.substring(16, 20) + "-" + value.substring(20));
    }

    private static UUID uuid7() {
        byte[] bytes = new byte[16]; RANDOM.nextBytes(bytes);
        long time = System.currentTimeMillis();
        for (int i = 5; i >= 0; i--) { bytes[i] = (byte) time; time >>>= 8; }
        bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x70);
        bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private static String vectorLiteral(float[] vector) {
        StringBuilder value = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) value.append(',');
            value.append(Float.toString(vector[i]));
        }
        return value.append(']').toString();
    }

    private static String jsonEscape(String text) {
        StringBuilder out = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b"); case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                default -> { if (c < 0x20) out.append(String.format("\\u%04x", (int) c)); else out.append(c); }
            }
        }
        return out.toString();
    }

    private static Map<String, String> readEnv(Path path) throws IOException {
        var values = new HashMap<String, String>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int separator = trimmed.indexOf('=');
            if (separator > 0) values.put(trimmed.substring(0, separator), trimmed.substring(separator + 1));
        }
        return values;
    }

    private static String required(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing configuration " + key);
        return value;
    }
}
