# chain-of-responsibility-pipeline
Chain of responsibility design pattern implementation inspired/learning from ATG pipeline concept with enhancements such as ForkJoin etc.

   This patterns avoids the layering of service invocations shown below,hence leaving us with flat processing structure which is easy to read, flexible.<br />
                `Controller`    
                   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- `Service A`     
                        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- `Service B`         
                        &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- `Service C`     
                   &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;- `Service E` 

### Pipeline concepts:

`Pipeline `: A user-defined chain process, which typically includes steps,stages for executing the required request.

`PipelineContext `: Mutable object which carries input,output,meta info across the pipeline steps,stages.

`Step `: A single executable task. Every step updates its execution results by mutating PipelineContext.
If `false` is returned from the step then remaining pipeline execution wil be skipped. 

`Stage `: A special type of step which conceptually includes distinct a subset of Steps (ex. `ForkJoinPipelineStage`).
If `false` is returned from the stage then remaining pipeline execution wil be skipped. 

`ForkJoinPipelineStage`: ForkJoinPipelineStage stage by default executes provided asyncSteps concurrently by creating its own thread pool and shutting it down once steps execution is complete, this is the preferred way as it is stateless. But this stage also supports/takes Executor service in case user wants to execute these asyncSteps in his own thread pool(**user is responsible for taking care of this thread pool tuning/life cycle)


### Pipeline Usage Example:

```
    PipelineStep s1 = pipelineContext -> {
          log.info("Executing step 1, threadName:" + Thread.currentThread().getName());
          return true;
        };

    PipelineStep s2 = pipelineContext -> {
          log.info("Executing step 2, threadName:" + Thread.currentThread().getName());
          return true;
        };

    PipelineStep s3 = pipelineContext -> {
          log.info("Executing step 3, threadName:" + Thread.currentThread().getName());
          return true;
        };

    PipelineStep s4 = pipelineContext -> {
          log.info("Executing step 4, threadName:" + Thread.currentThread().getName());
          return true;
        };

    // sequential steps executions s1,s2,s3,s4
    Pipeline pipeline = new Pipeline(s1, s2, s3, s4);
    PipelineContext pipelineContext = PipelineContext.builder().build();
    pipeline.execute(pipelineContext);

    // parallel steps execution s1,(s2,s3),s4
    ForkJoinPipelineStage forkJoinPipelineStage = new ForkJoinPipelineStage(s2, s3);
    Pipeline pipeline  = new Pipeline( s1, forkJoinPipelineStage, s4);
    PipelineContext pipelineContext1 = PipelineContext.builder().build();
    pipeline.execute(pipelineContext1);
```
## Pipeline & steps as Spring beans:

This code/jar didn't have any Spring dependency to be more flexible but if primary application which adds this jar as dependecny is Spring then both Pipeline & Steps can be Spring components as shown,

 > Extend PipelineContext to add custom/application specific properties

    public class CustomPipelineContext extends PipelineContext {
  
 > Extend PipelineStep to make it Spring bean

     @Component
     public class CustomPipelineStep implements PipelineStep {
     
 > Extend Pipeline to make it Spring bean & add steps in the PostConstruct method
```
   @Component
   public class CustomProcessingPipeline extends Pipeline {

   @PostConstruct
   public void afterConstruct() { 
          addPipelineSteps(
             customPipelineStep
```
         
 



