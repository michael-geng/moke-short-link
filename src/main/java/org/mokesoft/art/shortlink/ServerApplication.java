package org.mokesoft.art.shortlink;

import org.mokesoft.art.shortlink.service.ShortLinkBloomFilter;
import org.mokesoft.art.shortlink.vertx.SpringVerticleFactory;
import org.mokesoft.art.shortlink.vertx.SpringWorker;
import org.mokesoft.art.shortlink.web.RestApi;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

@SpringBootApplication
public class ServerApplication {

    private static final Logger logger = LoggerFactory.getLogger(ServerApplication.class);

    @Autowired
    private SpringVerticleFactory verticleFactory;

    @Value("${vertx.worker.pool.size: 1}")
    int workerPoolSize;

    @Value("${vertx.springWorker.instances: 1}")
    int springWorkerInstances;



    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(ServerApplication.class, args);
        ShortLinkBloomFilter shortLinkBloomFilter = context.getBean(ShortLinkBloomFilter.class);
        shortLinkBloomFilter.init();
    }

    @EventListener
    public void deployVerticles(ApplicationReadyEvent event) {
        Vertx vertx = Vertx.vertx(new VertxOptions()
                .setWorkerPoolSize(workerPoolSize).setMaxWorkerExecuteTime(10).setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS));

        vertx.registerVerticleFactory(verticleFactory);

        CountDownLatch deployLatch = new CountDownLatch(1);
        //多线程时使用该类可以提供原子性操作
        AtomicBoolean failed = new AtomicBoolean(false);
        String restApiVerticleName = verticleFactory.prefix() + ":" + RestApi.class.getName();
        vertx.deployVerticle(restApiVerticleName, ar -> {
            if (ar.failed()) {
                logger.error("Failed to deploy api verticle", ar.cause());
                failed.compareAndSet(false, true);
            }
            deployLatch.countDown();
        });

        DeploymentOptions workerDeploymentOptions = new DeploymentOptions()
                .setWorker(true)
                .setInstances(springWorkerInstances);
        String workerVerticleName = verticleFactory.prefix() + ":" + SpringWorker.class.getName();

        vertx.deployVerticle(workerVerticleName, workerDeploymentOptions, ar -> {
            if (ar.failed()) {
                logger.error("Failed to deploy verticle", ar.cause());
                failed.compareAndSet(false, true);
            }
            deployLatch.countDown();
        });

        try {
            //如果10秒还未发布成功则超时，抛出异常
            if (!deployLatch.await(10, SECONDS)) {
                //throw new RuntimeException("Timeout waiting for verticle deployments");
            } else if (failed.get()) {
                //throw new RuntimeException("Failure while deploying verticles");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
