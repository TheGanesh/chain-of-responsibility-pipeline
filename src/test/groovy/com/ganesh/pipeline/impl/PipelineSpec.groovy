package com.ganesh.pipeline.impl

import com.ganesh.pipeline.PipelineContext
import com.ganesh.pipeline.PipelineStep
import com.ganesh.pipeline.impl.test.SampleService
import com.ganesh.pipeline.impl.test.TestPipelineStep
import groovy.util.logging.Slf4j
import spock.lang.Specification

@Slf4j
class PipelineSpec extends Specification {

    def "Pipeline instantiation with sequential step processing"() {

        given:

        SampleService sampleService = Mock()

        PipelineStep s1 = { pipelineContext ->
            sampleService.sampleMethod()
            return true
        }

        PipelineStep s2 = { pipelineContext ->
            sampleService.sampleMethod()
            return true
        }

        PipelineStep s3 = { pipelineContext ->
            sampleService.sampleMethod()
            return true
        }

        PipelineStep s4 = { pipelineContext ->
            sampleService.sampleMethod()
            return true
        }
        PipelineContext pipelineContext = new PipelineContext()

        when:
        Pipeline Pipeline = new Pipeline(s1, s2, s3, s4)
        Pipeline.execute(pipelineContext)

        then:
        4 * sampleService.sampleMethod()
    }

    def "Pipeline instantiation with fork join step processing"() {

        given:

        SampleService sampleService = Mock()

        PipelineStep s1 = { pipelineContext ->
            sampleService.sampleMethod()
            return true
        }

        PipelineStep s2 = { pipelineContext ->
            sampleService.sampleMethod()
            return true
        }

        PipelineStep s3 = { pipelineContext ->
            sampleService.sampleMethod()
            return true
        }

        PipelineStep s4 = { pipelineContext ->
            sampleService.sampleMethod()
            return true
        }

        ForkJoinPipelineStage forkJoinPipelineStage = new ForkJoinPipelineStage(s2, s3)

        PipelineContext pipelineContext = new PipelineContext()

        when:

        Pipeline Pipeline = new Pipeline(s1, forkJoinPipelineStage, s4)
        Pipeline.execute(pipelineContext)

        then:
        4 * sampleService.sampleMethod()
    }

    def "Pipeline sequential step exception scenario"() {

        given:

        SampleService sampleService1 = Mock()
        SampleService sampleService2 = Mock()
        SampleService sampleService3 = Mock()

        PipelineStep s1 = new TestPipelineStep(sampleService: sampleService1)
        PipelineStep s2 = new TestPipelineStep(sampleService: sampleService2)
        PipelineStep s3 = new TestPipelineStep(sampleService: sampleService3)

        PipelineContext pipelineContext = new PipelineContext()

        when:
        Pipeline Pipeline = new Pipeline(s1, s2, s3)
        Pipeline.execute(pipelineContext)

        then:
        thrown(RuntimeException)
        1 * sampleService1.sampleMethod() >> true
        1 * sampleService2.sampleMethod() >> true
        1 * sampleService3.sampleMethod() >> { throw new RuntimeException() }
        0 * sampleService3.rollback() >> true
        1 * sampleService1.rollback() >> true
        1 * sampleService2.rollback() >> true


    }

    def "Pipeline sequential, one step causes other steps to skip"() {

        given:

        SampleService sampleService1 = Mock()
        SampleService sampleService2 = Mock()
        SampleService sampleService3 = Mock()

        PipelineStep s1 = new TestPipelineStep(sampleService: sampleService1)
        PipelineStep s2 = new TestPipelineStep(sampleService: sampleService2)
        PipelineStep s3 = new TestPipelineStep(sampleService: sampleService3)

        PipelineContext pipelineContext = new PipelineContext()

        when:
        Pipeline Pipeline = new Pipeline(s1, s2, s3)
        Pipeline.execute(pipelineContext)

        then:
        1 * sampleService1.sampleMethod() >> true
        1 * sampleService2.sampleMethod() >> false
        0 * sampleService3.sampleMethod()
        0 * sampleService3.rollback() >> true
        1 * sampleService1.rollback() >> true
        0 * sampleService2.rollback() >> true


    }
    def "ForkJoinPipelineStage rollback exception scenario"() {

        given:

        SampleService sampleService1 = Mock()
        SampleService sampleService2 = Mock()
        SampleService sampleService3 = Mock()
        SampleService sampleService4 = Mock()

        PipelineStep s1 = new TestPipelineStep(sampleService: sampleService1)
        PipelineStep s2 = new TestPipelineStep(sampleService: sampleService2)
        PipelineStep s3 = new TestPipelineStep(sampleService: sampleService3)
        PipelineStep s4 = new TestPipelineStep(sampleService: sampleService4)

        PipelineContext pipelineContext = new PipelineContext()

        when:
        Pipeline Pipeline = new Pipeline(new ForkJoinPipelineStage(s1, s2, s3), s4)
        Pipeline.execute(pipelineContext)

        then:
        thrown(RuntimeException)
        1 * sampleService1.sampleMethod() >> Boolean.TRUE
        1 * sampleService2.sampleMethod() >> Boolean.TRUE
        1 * sampleService3.sampleMethod() >> Boolean.TRUE
        1 * sampleService4.sampleMethod() >> { throw new RuntimeException() }

        0 * sampleService4.rollback()
        1 * sampleService1.rollback() >> Boolean.TRUE
        1 * sampleService2.rollback() >> Boolean.TRUE
        1 * sampleService3.rollback() >> Boolean.TRUE


    }

}
