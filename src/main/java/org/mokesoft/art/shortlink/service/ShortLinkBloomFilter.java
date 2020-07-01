package org.mokesoft.art.shortlink.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.sqlclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 使用guava的bloomfilter库
 * 启动后，异步从db加载初始数据。
 * 运行过程中，通过线程异步从db加载最新数据,10s一次
 */
@Component
public class ShortLinkBloomFilter {
    private static Logger logger = LoggerFactory.getLogger(ShortLinkBloomFilter.class);

    private static BloomFilter<String> bloomFilter = BloomFilter.create(
            Funnels.stringFunnel(Charset.defaultCharset()), 1000000);

    @Autowired
    private RepositoryConnection repositoryConnection;

    private volatile boolean isCompleted = false;

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    public void init() {
        if (!isCompleted){
            executorService.execute(fetchData);
        }
    }

    public Future<Boolean> mightContain(String hashValue) {
        Promise<Boolean> promise = Promise.promise();
        /*if (!isCompleted){
            this.init();
        }*/

        if ( isError.get() == true || (isCompleted == true && bloomFilter.mightContain(hashValue))){
            promise.complete(true);
        }else {
            promise.complete(false);
        }
        return promise.future();
    }

    public void put(String hashValue) {
        bloomFilter.put(hashValue);
    }

    private volatile long maxId = 0L;
    private final AtomicBoolean isError = new AtomicBoolean(false);

    private final Runnable fetchData = new Runnable() {
        @Override public void run() {
            Promise promise = Promise.promise();
            repositoryConnection.getPoolClient().getConnection(ar -> {
                if (!ar.succeeded()) {
                    isError.set(true);
                    logger.error("获取db连接失败{}", ar.cause());
                    return;
                }

                SqlConnection conn = ar.result();
                conn.prepare("select id, hash from short_link where id>? limit 1000", ar1 -> {
                    if (ar1.succeeded()) {
                        PreparedStatement pq = ar1.result();
                        RowStream<Row> stream = pq.createStream(1000, Tuple.of(maxId));

                        stream.exceptionHandler(err -> {
                            System.out.println("stream取数据 Error: " + err.getMessage());
                            pq.close();
                        }).endHandler(v -> {
                            isError.set(false);
                            pq.close();
                            promise.complete();
                        }).handler(row -> {
                            maxId = row.getInteger("id");
                            bloomFilter.put(row.getString("hash"));
                        });
                    }else{
                        logger.error("bloomfilter数据加载错误：{}", ar1.cause());
                    }
                    conn.close();
                });
            });

            promise.future().onComplete(res->{
                if (!isCompleted){
                    isCompleted = true;
                    executorService.scheduleAtFixedRate(fetchData,1, 2, TimeUnit.SECONDS);
                }
            });

        }
    };
}