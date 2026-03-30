package io.jaiclaw.cron

import io.jaiclaw.core.model.CronJob
import io.jaiclaw.core.model.CronJobResult
import io.jaiclaw.core.tenant.DefaultTenantContext
import io.jaiclaw.core.tenant.TenantContextHolder
import io.jaiclaw.core.tenant.TenantGuard
import io.jaiclaw.core.tenant.TenantMode
import io.jaiclaw.core.tenant.TenantProperties
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Path

class CronServiceMultiTenantSpec extends Specification {

    def multiGuard = new TenantGuard(new TenantProperties(TenantMode.MULTI, "default"))

    @TempDir
    Path tempDir

    JsonFileCronJobStore jobStore
    CronJobExecutor executor
    CronService service

    def setup() {
        jobStore = new JsonFileCronJobStore(tempDir.resolve("cron-jobs.json"), multiGuard)
        executor = new CronJobExecutor({ job -> "executed: ${job.name()}" })
        service = new CronService(jobStore, executor, 2, 30, multiGuard)
    }

    def setTenant(String id) {
        TenantContextHolder.set(new DefaultTenantContext(id, id))
    }

    def cleanup() {
        TenantContextHolder.clear()
    }

    private CronJob makeJob(String id, String tenantId) {
        CronJob.builder()
                .id(id)
                .name("job-$id")
                .agentId("agent-1")
                .schedule("0 0 * * *")
                .timezone("UTC")
                .prompt("test prompt")
                .enabled(true)
                .tenantId(tenantId)
                .build()
    }

    def "listJobs filtered by tenant"() {
        given:
        setTenant("tenant-a")
        service.addJob(makeJob("a-1", "tenant-a"))
        service.addJob(makeJob("a-2", "tenant-a"))

        setTenant("tenant-b")
        service.addJob(makeJob("b-1", "tenant-b"))

        when:
        setTenant("tenant-a")
        def jobsA = service.listJobs()

        then:
        jobsA.size() == 2
        jobsA.every { it.tenantId() == "tenant-a" }

        when:
        setTenant("tenant-b")
        def jobsB = service.listJobs()

        then:
        jobsB.size() == 1
        jobsB[0].tenantId() == "tenant-b"
    }

    def "runNow as wrong tenant fails with 'Job not found'"() {
        given:
        setTenant("tenant-a")
        service.addJob(makeJob("run-1", "tenant-a"))

        when:
        setTenant("tenant-b")
        def result = service.runNow("run-1")

        then:
        result instanceof CronJobResult.Failure
        (result as CronJobResult.Failure).error() == "Job not found"
    }
}
