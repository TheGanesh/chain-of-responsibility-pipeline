package com.ganesh.forkjoin;

import static org.apache.commons.collections.MapUtils.isNotEmpty;


import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import org.slf4j.MDC;

public class ForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {

  // we want the same values given to all child threads
  private Map<String, String> mdcContextCopy = MDC.getCopyOfContextMap();

  @Override
  public ForkJoinWorkerThread newThread(ForkJoinPool forkJoinPool) {
    return new ForkJoinWorkerThread(forkJoinPool) {
      @Override
      protected void onStart() {
        if (isNotEmpty(mdcContextCopy)) {
          mdcContextCopy.forEach(MDC::put);
        }
        super.onStart();
      }

      @Override
      protected void onTermination(Throwable exception) {
        MDC.clear();
        super.onTermination(exception);
      }
    };
  }

}