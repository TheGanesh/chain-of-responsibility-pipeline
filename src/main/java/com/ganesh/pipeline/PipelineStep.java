package com.ganesh.pipeline;

import java.util.function.Function;

@FunctionalInterface
public interface PipelineStep extends Function<PipelineContext, Boolean> {

  @Override
  Boolean apply(PipelineContext pipelineContext);

  default Boolean isApplicable(PipelineContext pipelineContext) {
    return true;
  }

  default Boolean reverse(PipelineContext pipelineContext) {
    return true;
  }

}
