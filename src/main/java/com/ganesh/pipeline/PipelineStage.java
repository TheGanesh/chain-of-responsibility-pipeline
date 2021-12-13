package com.ganesh.pipeline;

import java.util.List;

public interface PipelineStage extends PipelineStep {

  List<PipelineStep> getSubSteps();

}
