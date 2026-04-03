package io.jaiclaw.maven

import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugin.logging.Log
import spock.lang.Specification
import spock.lang.TempDir

import java.lang.reflect.Field
import java.nio.file.Files
import java.nio.file.Path

class AnalyzeMojoSpec extends Specification {

    @TempDir
    Path tempDir

    Log mockLog = Mock()

    private AnalyzeMojo createMojo(Map<String, Object> params = [:]) {
        AnalyzeMojo mojo = new AnalyzeMojo()
        mojo.setLog(mockLog)

        File baseDirValue = params.containsKey('baseDir')
                ? params.baseDir as File
                : tempDir.toFile()
        setField(mojo, 'baseDir', baseDirValue)
        setField(mojo, 'threshold', params.getOrDefault('threshold', 0) as int)
        setField(mojo, 'skip', params.getOrDefault('skip', false) as boolean)
        setField(mojo, 'failOnWarning', params.getOrDefault('failOnWarning', false) as boolean)
        return mojo
    }

    private static void setField(Object target, String name, Object value) {
        Field field = AnalyzeMojo.getDeclaredField(name)
        field.setAccessible(true)
        field.set(target, value)
    }

    private Path createProject(String appYml) {
        Path resources = tempDir.resolve("src/main/resources")
        Files.createDirectories(resources)
        Files.writeString(resources.resolve("application.yml"), appYml)
        return tempDir
    }

    def "runs analysis and prints report with valid application.yml"() {
        given:
        createProject("""
jaiclaw:
  skills:
    allow-bundled: []
  agent:
    agents:
      default:
        tools:
          profile: full
""")
        AnalyzeMojo mojo = createMojo()

        when:
        mojo.execute()

        then:
        noExceptionThrown()
        (1.._) * mockLog.info(_ as CharSequence)
    }

    def "skip=true exits without analysis"() {
        given:
        AnalyzeMojo mojo = createMojo(skip: true)

        when:
        mojo.execute()

        then:
        noExceptionThrown()
        1 * mockLog.info("JaiClaw prompt analysis skipped")
        0 * mockLog.info({ it?.toString()?.contains("Prompt Token Analysis") })
    }

    def "missing application.yml causes silent skip"() {
        given:
        // tempDir has no src/main/resources/application.yml
        AnalyzeMojo mojo = createMojo()

        when:
        mojo.execute()

        then:
        noExceptionThrown()
        0 * mockLog.info({ it?.toString()?.contains("Prompt Token Analysis") })
    }

    def "threshold exceeded causes MojoFailureException"() {
        given:
        createProject("""
jaiclaw:
  skills:
    allow-bundled: []
  agent:
    agents:
      default:
        tools:
          profile: full
""")
        AnalyzeMojo mojo = createMojo(threshold: 1)

        when:
        mojo.execute()

        then:
        MojoFailureException ex = thrown()
        ex.message.contains("exceeds threshold")
    }

    def "failOnWarning=true with missing allow-bundled causes MojoFailureException"() {
        given:
        createProject("""
jaiclaw:
  agent:
    agents:
      default:
        tools:
          profile: full
""")
        AnalyzeMojo mojo = createMojo(failOnWarning: true)

        when:
        mojo.execute()

        then:
        MojoFailureException ex = thrown()
        ex.message.contains("warning")
    }

    def "failOnWarning=false with warnings does not fail"() {
        given:
        createProject("""
jaiclaw:
  agent:
    agents:
      default:
        tools:
          profile: full
""")
        AnalyzeMojo mojo = createMojo(failOnWarning: false)

        when:
        mojo.execute()

        then:
        noExceptionThrown()
    }
}
