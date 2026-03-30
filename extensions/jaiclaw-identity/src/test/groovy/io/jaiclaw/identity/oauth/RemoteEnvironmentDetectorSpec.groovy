package io.jaiclaw.identity.oauth

import spock.lang.Specification

class RemoteEnvironmentDetectorSpec extends Specification {

    def "isRemote returns a boolean without throwing"() {
        when:
        boolean result = RemoteEnvironmentDetector.isRemote()

        then:
        // On a developer machine, this should be false
        // The main thing we're testing is that it doesn't throw
        noExceptionThrown()
        result instanceof Boolean
    }
}
