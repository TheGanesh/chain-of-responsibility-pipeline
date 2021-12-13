package com.ganesh.pipeline;

/**
 * Subclass will contain/carry all the input/output related objects which are independent of this pipeline execution
 * Input input; // input to the pipeline by converting REST request
 * Output output; // output of the pipeline execution, will mapped back to the REST response
 */

public class PipelineContext {

  public PipelineInput pipelineInput;

}
