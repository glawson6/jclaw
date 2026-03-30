package io.jaiclaw.identity.secret

import io.jaiclaw.core.auth.SecretRefSource
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission

class FileSecretProviderSpec extends Specification {

    @TempDir
    Path tempDir

    FileSecretProvider provider = new FileSecretProvider()

    private Path writeSecretFile(String name, String content) {
        Path file = tempDir.resolve(name)
        Files.writeString(file, content)
        // Set restrictive permissions so the provider doesn't reject it
        try {
            Files.setPosixFilePermissions(file, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE))
        } catch (UnsupportedOperationException ignored) {
            // Not a POSIX filesystem
        }
        return file
    }

    def "resolves JSON pointer from file"() {
        given:
        Path secretFile = writeSecretFile("secrets.json",
                '{"providers":{"openai":{"apiKey":"sk-test123"}}}')

        SecretProviderConfig config = new SecretProviderConfig(
                SecretRefSource.FILE, "test", null,
                secretFile.toString(), SecretProviderConfig.MODE_JSON, false,
                SecretProviderConfig.DEFAULT_TIMEOUT_MS, SecretProviderConfig.DEFAULT_MAX_BYTES,
                null, null, false, Map.of(), List.of(tempDir.toString()))

        when:
        String value = provider.resolve("/providers/openai/apiKey", config)

        then:
        value == "sk-test123"
    }

    def "resolves singleValue mode"() {
        given:
        Path secretFile = writeSecretFile("api-key.txt", "sk-single-value\n")

        SecretProviderConfig config = new SecretProviderConfig(
                SecretRefSource.FILE, "test", null,
                secretFile.toString(), SecretProviderConfig.MODE_SINGLE_VALUE, false,
                SecretProviderConfig.DEFAULT_TIMEOUT_MS, SecretProviderConfig.DEFAULT_MAX_BYTES,
                null, null, false, Map.of(), List.of(tempDir.toString()))

        when:
        String value = provider.resolve("ignored", config)

        then:
        value == "sk-single-value"
    }

    def "throws for nonexistent file"() {
        given:
        SecretProviderConfig config = new SecretProviderConfig(
                SecretRefSource.FILE, "test", null,
                tempDir.resolve("nonexistent.json").toString(), SecretProviderConfig.MODE_JSON, false,
                SecretProviderConfig.DEFAULT_TIMEOUT_MS, SecretProviderConfig.DEFAULT_MAX_BYTES,
                null, null, false, Map.of(), List.of(tempDir.toString()))

        when:
        provider.resolve("/key", config)

        then:
        thrown(SecretRefResolver.SecretResolutionException)
    }

    def "throws for missing JSON pointer path"() {
        given:
        Path secretFile = writeSecretFile("secrets.json", '{"providers":{}}')

        SecretProviderConfig config = new SecretProviderConfig(
                SecretRefSource.FILE, "test", null,
                secretFile.toString(), SecretProviderConfig.MODE_JSON, false,
                SecretProviderConfig.DEFAULT_TIMEOUT_MS, SecretProviderConfig.DEFAULT_MAX_BYTES,
                null, null, false, Map.of(), List.of(tempDir.toString()))

        when:
        provider.resolve("/providers/openai/apiKey", config)

        then:
        thrown(SecretRefResolver.SecretResolutionException)
    }
}
