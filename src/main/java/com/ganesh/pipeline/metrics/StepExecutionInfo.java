package com.ganesh.pipeline.metrics;

import com.ganesh.pipeline.PipelineStep;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class StepExecutionInfo {

  PipelineStep pipelineStep;
  long executionTime;
  ExecutionStatus executionStatus;

  @Override
  public String toString() {
    return "{" +
        "pipelineStep=" + pipelineStep.getClass().getSimpleName() +
        ", executionTime=" + executionTime +
        ", executionStatus=" + executionStatus +
        "}";
  }
}


