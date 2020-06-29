package org.mokesoft.art.shortlink.service;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.TimeUnit;

@Component
public class RepositoryConnection {
    private static final Logger logger = LoggerFactory.getLogger(RepositoryConnection.class);

    private MySQLPool pool;

    @Value("${mysql.host}")
    private String host;

    @Value("${mysql.port}")
    private Integer port;

    @Value("${mysql.database}")
    private String database;

    @Value("${mysql.user}")
    private String user;

    @Value("${mysql.password}")
    private String password;

    @Value("${mysql.max-size:5}")
    private Integer maxSize;


    private volatile boolean isCompleted = false;

    @PostConstruct
    public void init() {
        if (!isCompleted) {
            synchronized (RepositoryConnection.class) {
                if (!isCompleted) {
                    isCompleted = true;
                    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                            .setPort(port)
                            .setHost(host)
                            .setDatabase(database)
                            .setUser(user)
                            .setPassword(password)
                            .setCachePreparedStatements(true)
                            .setPreparedStatementCacheMaxSize(100)
                            .setCharset("utf8mb4")
                            .addProperty("useUnicode", "true")
                            .addProperty("allowMultiQueries", "true")
                            .setConnectTimeout(3000)
                            .setIdleTimeout(3000)
                            .setIdleTimeoutUnit(TimeUnit.SECONDS);

                    PoolOptions poolOptions = new PoolOptions()
                            .setMaxSize(20);

                    pool = MySQLPool.pool(connectOptions, poolOptions);
                    logger.info("初始化数据库连接池");
                }

            }

        }
    }

    public MySQLPool getPoolClient() {
        if (!isCompleted) {
            this.init();
        }
        return this.pool;
    }

    public Future<SqlConnection> getConnection() {
        Promise<SqlConnection> promise = Promise.promise();
        this.getPoolClient().getConnection(res -> {
            if (res.succeeded()) {
                promise.complete(res.result());
            } else {
                logger.error("获取db connection 失败: {}", res.cause().getMessage());
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    public Future<RowSet<Row>> getRows(String sql) {
        Promise<RowSet<Row>> promise = Promise.promise();

        this.getPoolClient().getConnection(res->{
            if (res.succeeded()){
                SqlConnection connection = res.result();
                connection.query(sql).execute(ar -> {
                    if (ar.succeeded()) {
                        promise.complete(ar.result());
                    } else {
                        promise.fail(ar.cause());
                    }
                    connection.close();
                });
            }else{
                logger.error("获取db connection 失败: {}", res.cause().getMessage());
                promise.fail(res.cause());
            }
        });

        return promise.future();
    }

    /**update
     * 执行更新语句
     */
    public void update(String sql, Tuple param, Handler<AsyncResult<Integer>> resultHandler) {
        Promise promise = Promise.promise();

        this.getPoolClient().getConnection(res->{
            if (res.succeeded()){
                SqlConnection connection = res.result();
                connection.preparedQuery(sql).execute(param, ar -> {
                    if (ar.succeeded()) {
                        promise.complete(ar.result().rowCount());
                    } else {
                        promise.fail(ar.cause());
                    }
                    connection.close();
                });
            }else{
                logger.error("获取db connection 失败: {}", res.cause().getMessage());
                promise.fail(res.cause());
            }
        });
        promise.future().onComplete(resultHandler);
    }

    @PreDestroy
    public void destroy() {
        this.pool.close();
    }
}
