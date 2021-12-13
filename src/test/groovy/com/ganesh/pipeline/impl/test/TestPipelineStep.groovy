package com.ganesh.pipeline.impl.test

import com.ganesh.pipeline.PipelineStep
import com.ganesh.pipeline.PipelineContext

class TestPipelineStep implements PipelineStep{

    SampleService sampleService

    @Override
    Boolean apply(PipelineContext pipelineContext) throws Exception {
        return sampleService.sampleMethod()
    }

    @Override
    Boolean reverse(PipelineContext pipelineContext) {
        return sampleService.rollback()
    }
}
