package org.mokesoft.art.shortlink;

import io.vertx.core.*;

public class TestMain extends AbstractVerticle {

    @Override
    public void start() throws Exception{
        WorkerExecutor executor = vertx.createSharedWorkerExecutor("test-work-executor");
        executor.executeBlocking(promise -> {

            promise.complete("hello world");
        }, result ->{
            System.out.println(result.result());
        });

    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(2).setMaxEventLoopExecuteTime(Long.MAX_VALUE));

        vertx.deployVerticle(new TestMain(), res ->{
            if (res.succeeded()){
                System.out.println("deploy succeed");
            }else {
                System.out.println("deploy fail");
            }
        });
    }

    public void test(){
        Future future = Future.future();
        future.compose(v->{
          Future future1 = Future.future();
          return future1;
        }).compose(v ->{

        }, future);

    }

}
