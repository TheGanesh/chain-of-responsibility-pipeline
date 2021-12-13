package com.ganesh.pipeline.impl;

import static java.util.stream.Collectors.joining;

import com.ganesh.pipeline.IPipeline;
import com.ganesh.pipeline.PipelineContext;
import com.ganesh.pipeline.PipelineStep;
import com.ganesh.pipeline.metrics.ExecutionStatus;
import com.ganesh.pipeline.metrics.StepExecutionInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
public class Pipeline implements IPipeline {

  private List<PipelineStep> pipelineSteps = new ArrayList<>();

  public Pipeline(PipelineStep... pipelineSteps) {
    this.pipelineSteps = Arrays.asList(pipelineSteps);
  }

  @Override
  public List<PipelineStep> getPipelineSteps() {
    return pipelineSteps;
  }

  public void addPipelineSteps(PipelineStep... pipelineSteps) {
    this.pipelineSteps.addAll(Arrays.asList(pipelineSteps));
  }

  @Override
  public void execute(PipelineContext pipelineContext) {

    List<StepExecutionInfo> stepsExecutionInfo = new ArrayList<>();

    long pipelineStartTime = System.currentTimeMillis();
    ExecutionStatus pipelineExecutionStatus = ExecutionStatus.success;

    try {

      for (PipelineStep pipelineStep : pipelineSteps) {

        if (pipelineExecutionStatus == ExecutionStatus.skipped || !pipelineStep.isApplicable(pipelineContext)) {
          stepsExecutionInfo.add(0, new StepExecutionInfo(pipelineStep, 0, ExecutionStatus.skipped));
          continue;
        }

        long stepStartTime = System.currentTimeMillis();
        ExecutionStatus stepExecutionStatus = ExecutionStatus.success;

        try {
          Boolean proceedWithPipeline = pipelineStep.apply(pipelineContext);
          if (!proceedWithPipeline) {
            log.info("step=" + pipelineStep.getClass().getSimpleName() + " resulted in skip remaining pipeline execution " + getPipelineName());
            pipelineExecutionStatus = ExecutionStatus.skipped;
            stepExecutionStatus = ExecutionStatus.skipped;
          }
        } catch (Exception ex) {
          stepExecutionStatus = ExecutionStatus.failed;
          log.warn("action=PipelineExecutionException,pipeline=" + getPipelineName() + ",stepFailed=" + pipelineStep.getClass().getSimpleName(), ex);
          throw ex;
        } finally {
          long stepExecutionTime = System.currentTimeMillis() - stepStartTime;
          stepsExecutionInfo.add(0, new StepExecutionInfo(pipelineStep, stepExecutionTime, stepExecutionStatus));
        }

      }
    } catch (Exception exception) {
      pipelineExecutionStatus = ExecutionStatus.failed;
      throw exception;
    } finally {
      long pipelineExecutionTime = System.currentTimeMillis() - pipelineStartTime;
      String stepsInfo = stepsExecutionInfo.stream().map(StepExecutionInfo::toString).collect(joining(","));
      if (pipelineExecutionStatus != ExecutionStatus.success) {
        rollbackPreviousSuccessfulSteps(stepsExecutionInfo, pipelineContext);
      }
      log.info("pipeline=" + getPipelineName() + ",pipelineExecutionStatus=" + pipelineExecutionStatus + ",pipelineExecutionTime=" + pipelineExecutionTime + ",stepsInfo=" + stepsInfo);
    }
  }

  void rollbackPreviousSuccessfulSteps(List<StepExecutionInfo> stepsExecutionInfo, PipelineContext pipelineContext) {
    stepsExecutionInfo
        .stream()
        .filter(stepExecutionInfo -> stepExecutionInfo.getExecutionStatus() == ExecutionStatus.success)
        .forEach(stepExecutionInfo -> {
          try {
            stepExecutionInfo.getPipelineStep().reverse(pipelineContext);
          } catch (Exception e) {
            log.error("unable to call reverse() for " + stepExecutionInfo.getPipelineStep() + " skipping to next.");
          }
        });
  }

  public String getPipelineName() {
    return this.getClass().getSimpleName();
  }
}
