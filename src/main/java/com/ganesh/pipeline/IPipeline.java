package com.ganesh.pipeline;

import java.util.List;

public interface IPipeline {

  List<PipelineStep> getPipelineSteps();

  void execute(PipelineContext pipelineContext);

}
