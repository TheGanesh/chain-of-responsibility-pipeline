package com.ganesh.pipeline.impl

import com.ganesh.pipeline.PipelineContext
import com.ganesh.pipeline.PipelineStep
import com.ganesh.pipeline.impl.test.SampleService
import com.ganesh.pipeline.impl.test.TestPipelineStep
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class ForkJoinPipelineStageSpec extends Specification {

    def "ForkJoinPipelineStage success scenario"() {

        given:

        SampleService sampleService1 = Mock()
        SampleService sampleService2 = Mock()
        SampleService sampleService3 = Mock()

        PipelineStep s1 = new TestPipelineStep(sampleService: sampleService1)
        PipelineStep s2 = new TestPipelineStep(sampleService: sampleService2)
        PipelineStep s3 = new TestPipelineStep(sampleService: sampleService3)

        PipelineContext pipelineContext = new PipelineContext()
        when:
        Boolean result = new ForkJoinPipelineStage(s1, s2, s3).apply(pipelineContext)

        then:
        result
        1 * sampleService1.sampleMethod() >> Boolean.TRUE
        1 * sampleService2.sampleMethod() >> Boolean.TRUE
        1 * sampleService3.sampleMethod() >> Boolean.TRUE
        0 * sampleService3.rollback()
        0 * sampleService1.rollback()
        0 * sampleService2.rollback()


    }

    def "ForkJoinPipelineStage success scenario - custom thread pool"() {

        given:

        SampleService sampleService1 = Mock()
        SampleService sampleService2 = Mock()
        SampleService sampleService3 = Mock()

        PipelineStep s1 = new TestPipelineStep(sampleService: sampleService1)
        PipelineStep s2 = new TestPipelineStep(sampleService: sampleService2)
        PipelineStep s3 = new TestPipelineStep(sampleService: sampleService3)

        PipelineContext pipelineContext = new PipelineContext()
        ExecutorService executorService = Mock()
        Future<Boolean> future = Mock()

        when:
        Boolean result = new ForkJoinPipelineStage(executorService, s1, s2, s3).apply(pipelineContext)

        then:
        result

        3 * executorService.submit(_ as Callable) >> { Callable callable ->
            callable.call()
            return future
        }
        3 * future.get() >> Boolean.TRUE
        1 * sampleService1.sampleMethod() >> Boolean.TRUE
        1 * sampleService2.sampleMethod() >> Boolean.TRUE
        1 * sampleService3.sampleMethod() >> Boolean.TRUE
        0 * sampleService3.rollback()
        0 * sampleService1.rollback()
        0 * sampleService2.rollback()


    }

    def "ForkJoinPipelineStage exception scenario"() {

        given:

        SampleService sampleService1 = Mock()
        SampleService sampleService2 = Mock()
        SampleService sampleService3 = Mock()

        PipelineStep s1 = new TestPipelineStep(sampleService: sampleService1)
        PipelineStep s2 = new TestPipelineStep(sampleService: sampleService2)
        PipelineStep s3 = new TestPipelineStep(sampleService: sampleService3)

        PipelineContext pipelineContext = new PipelineContext()
        when:
        new ForkJoinPipelineStage(s1, s2, s3).apply(pipelineContext)

        then:
        thrown(RuntimeException)
        1 * sampleService1.sampleMethod() >> Boolean.TRUE
        1 * sampleService2.sampleMethod() >> Boolean.TRUE
        1 * sampleService3.sampleMethod() >> { throw new RuntimeException() }
        0 * sampleService3.rollback()
        1 * sampleService1.rollback() >> Boolean.TRUE
        1 * sampleService2.rollback() >> Boolean.TRUE


    }

    def "ForkJoinPipelineStage one step returning false from apply method"() {

        given:

        SampleService sampleService1 = Mock()
        SampleService sampleService2 = Mock()
        SampleService sampleService3 = Mock()

        PipelineStep s1 = new TestPipelineStep(sampleService: sampleService1)
        PipelineStep s2 = new TestPipelineStep(sampleService: sampleService2)
        PipelineStep s3 = new TestPipelineStep(sampleService: sampleService3)

        PipelineContext pipelineContext = new PipelineContext()
        when:
        Boolean result = new ForkJoinPipelineStage(s1, s2, s3).apply(pipelineContext)

        then:
        !result
        1 * sampleService1.sampleMethod() >> Boolean.TRUE
        1 * sampleService2.sampleMethod() >> Boolean.FALSE
        1 * sampleService3.sampleMethod() >> Boolean.TRUE
        1 * sampleService3.rollback() >> Boolean.TRUE
        1 * sampleService1.rollback() >> Boolean.TRUE
        0 * sampleService2.rollback() >> Boolean.TRUE


    }
}
