package com.ganesh.forkjoin;


import static java.util.stream.Collectors.toMap;


import com.ganesh.pipeline.PipelineContext;
import com.ganesh.pipeline.PipelineStep;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ForkJoinUtil {

  public static Map<PipelineStep, Future<Boolean>> runWithExistingPool(ExecutorService executorService, List<PipelineStep> pipelineSteps, PipelineContext pipelineContext) {
    return
        pipelineSteps
            .stream()
            .collect(
                toMap(
                    pipelineStep -> pipelineStep,
                    pipelineStep -> executorService.submit(() -> pipelineStep.apply(pipelineContext))
                )
            );

  }

  public static Map<PipelineStep, Future<Boolean>> runWithNewForkJoinPool(List<PipelineStep> pipelineSteps, PipelineContext pipelineContext) throws InterruptedException {

    ForkJoinPool forkJoinPool = new ForkJoinPool(pipelineSteps.size(), new ForkJoinWorkerThreadFactory(), null, false);

    try {

      return runWithExistingPool(forkJoinPool, pipelineSteps, pipelineContext);

    } finally {
      forkJoinPool.shutdown();
      forkJoinPool.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
    }
  }

}
