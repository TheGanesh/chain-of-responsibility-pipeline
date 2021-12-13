package com.ganesh.pipeline.impl;

import static com.ganesh.forkjoin.ForkJoinUtil.runWithExistingPool;
import static com.ganesh.forkjoin.ForkJoinUtil.runWithNewForkJoinPool;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


import com.ganesh.pipeline.PipelineContext;
import com.ganesh.pipeline.PipelineStage;
import com.ganesh.pipeline.PipelineStep;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ForkJoinPipelineStage implements PipelineStage {

  private ExecutorService executorService;

  List<PipelineStep> asyncSteps;

  @Override
  public List<PipelineStep> getSubSteps() {
    return asyncSteps;
  }

  public ForkJoinPipelineStage(PipelineStep... asyncSteps) {
    this.asyncSteps = Arrays.asList(asyncSteps);
  }

  public ForkJoinPipelineStage(ExecutorService executorService, PipelineStep... asyncSteps) {
    this.executorService = executorService;
    this.asyncSteps = Arrays.asList(asyncSteps);
  }

  @Override
  public Boolean isApplicable(PipelineContext pipelineContext) {
    return !asyncSteps.isEmpty();
  }

  @SneakyThrows
  @Override
  public Boolean apply(PipelineContext pipelineContext) {

    List<PipelineStep> applicableSteps = getApplicableSteps(pipelineContext);
    if (applicableSteps.isEmpty()) {
      return true;
    } else if (applicableSteps.size() == 1) {
      // If only one component just run it instead of creating unnecessary ThreadPool
      return applicableSteps.get(0).apply(pipelineContext);
    }

    Map<PipelineStep, Future<Boolean>> currentlyRunningStepsFutures = executorService != null ?
        runWithExistingPool(executorService, applicableSteps, pipelineContext) : runWithNewForkJoinPool(applicableSteps, pipelineContext);

    // waiting for all responses/Futures
    Map<PipelineStep, Object> stepsExecutionResults = currentlyRunningStepsFutures
        .entrySet()
        .stream()
        .collect(
            toMap(
                Map.Entry::getKey,
                entry -> extractResult(entry.getKey(), entry.getValue()))
        );

    //Handling any step failure
    Optional<Object> exceptionOptional = stepsExecutionResults
        .values()
        .stream()
        .filter(result -> result instanceof Exception)
        .findFirst();

    // at least one step failed so reversing all other successful steps
    // throwing first exception back to interrupt pipeline flow as usual
    if (exceptionOptional.isPresent()) {
      reverseAllSuccessfulSteps(stepsExecutionResults, pipelineContext);
      throw (Exception) exceptionOptional.get();
    }

    //handling any step returning false from apply() method to interrupt pipeline flow
    Optional<Object> skippedStepsOptional = stepsExecutionResults
        .values()
        .stream()
        .filter(result -> result == Boolean.FALSE)
        .findFirst();
    if (skippedStepsOptional.isPresent()) {
      reverseAllSuccessfulSteps(stepsExecutionResults, pipelineContext);
      return false;
    }

    return true;
  }

  private void reverseAllSuccessfulSteps(Map<PipelineStep, Object> stepsExecutionResults, PipelineContext pipelineContext) {
    stepsExecutionResults
        .entrySet()
        .stream()
        .filter(entry -> entry.getValue() == Boolean.TRUE)
        .forEach(entry -> entry.getKey().reverse(pipelineContext));
  }

  @Override
  public Boolean reverse(PipelineContext pipelineContext) {
    List<PipelineStep> applicableSteps = getApplicableSteps(pipelineContext);

    if (applicableSteps.isEmpty()) {
      return true;
    }
    applicableSteps.forEach(pipelineStep -> {
      try {
        pipelineStep.reverse(pipelineContext);
      } catch (Exception e) {
        log.error("unable to call reverse() for " + pipelineStep + " skipping to next.");
      }
    });

    return true;
  }

  List<PipelineStep> getApplicableSteps(PipelineContext pipelineContext) {
    return asyncSteps
        .stream()
        .filter(pipelineStep -> pipelineStep.isApplicable(pipelineContext))
        .collect(toList());
  }

  private Object extractResult(PipelineStep pipelineStep, Future<Boolean> booleanFuture) {
    try {
      return booleanFuture.get();
    } catch (Exception exception) {
      log.error("action=ExecutionException,pipelineStep=" + pipelineStep.getClass().getSimpleName(), exception);
      return exception.getCause();
    }
  }

}
