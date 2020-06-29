package org.mokesoft.art.shortlink.service.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.hash.Hashing;
import org.mokesoft.art.shortlink.entity.ShortLink;
import org.mokesoft.art.shortlink.form.ShortLinkForm;
import org.mokesoft.art.shortlink.service.RepositoryConnection;
import org.mokesoft.art.shortlink.service.ShortLinkAsyncService;
import org.mokesoft.art.shortlink.service.ShortLinkBloomFilter;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Component
public class ShortLinkAsyncServiceImpl implements ShortLinkAsyncService {
    private static final Logger logger = LoggerFactory.getLogger(ShortLinkAsyncServiceImpl.class);
    private static final String URL_SUFFIX = "[SUFFIX]";

    @Value("${short-link-domain:}")
    private String shortLinkDomain;

    @Autowired
    private RepositoryConnection repositoryConnection;

    @Autowired
    private ShortLinkBloomFilter shortLinkBloomFilter;

    private Cache<String, String> localCache = CacheBuilder.newBuilder().maximumSize(1000)
            .expireAfterAccess(1000, TimeUnit.SECONDS).build();

    /**
     * 如果写入遇到瓶颈，可以先通过bloomfilter进行筛选，然后直接insert
     */
    @Override
    public void save(ShortLinkForm form, Handler<AsyncResult<String>> resultHandler) {
        Promise<String> promise = Promise.promise();

        Long expired = 0L;
        if (!"0".equals(form.getExpired())) {
            expired = System.currentTimeMillis() + Integer.valueOf(form.getExpired()) * 30 * 24 * 60 * 1000;
        }

        Promise<ShortLink> promiseHash = Promise.promise();

        final ShortLink entity = new ShortLink(form.getUrl(), hash(form.getUrl()), expired);

        Future<RowSet<Row>> future1 = repositoryConnection.getRows("select * from short_link where hash='"+entity.getHash()+"'");
        future1.compose(v->{
            //如果存在，并且url不相同，重新获取hash值
            boolean hasV = false;
            if (v.size() > 0){
                for (Row row : v) {
                    if (row.getString("origin_url").equalsIgnoreCase(form.getUrl())) {
                        hasV = true;
                        break;
                    }
                }
                if (hasV){
                    entity.setNew(false);
                    promiseHash.complete(entity);
                }else{
                    entity.setHash(hash(entity.getOriginUrl() + URL_SUFFIX));
                }
            }else{
                promiseHash.complete(entity);
            }
            return promiseHash.future();
        }).compose(res->{
            //需要新增
            if (res.getNew() == true){
                String sql = "insert into short_link(origin_url,hash,expired,create_time,update_time) VALUES(?, ?, ?, now(), now())";
                repositoryConnection.update(sql, Tuple.of(entity.getOriginUrl(), entity.getHash(), entity.getExpired()), result -> {
                    if (result.succeeded()){
                        logger.info("insert success " + result.result());
                        shortLinkBloomFilter.put(entity.getHash());
                        promise.complete(shortLinkDomain + entity.getHash());
                    }else{
                        promise.fail(result.cause());
                    }
                });
            }else{
                promise.complete(shortLinkDomain + res.getHash());
            }
        }, promise.future().onComplete(resultHandler));
    }

    @Override
    public void get(final String hash, Handler<AsyncResult<String>> resultHandler) {
        Promise<String> promise = Promise.promise();

        Future<Boolean> fu1 = shortLinkBloomFilter.mightContain(hash);

        fu1.compose(v -> {
            boolean val = v.booleanValue();
            if (val) {
                //先从缓存查找
                String url = localCache.getIfPresent(hash);
                if (url == null) {
                    repositoryConnection.getPoolClient().getConnection(ar1 -> {
                        if (ar1.succeeded()) {
                            SqlConnection conn = ar1.result();
                            conn.query("select * from short_link where hash='"+hash+"'")
                                    .execute( ar -> {
                                        if (ar.succeeded()) {
                                            String originUrl = "";
                                            RowSet<Row> result = ar.result();
                                            for (Row row : result) {
                                                localCache.put(hash, row.getString("origin_url"));
                                                originUrl=row.getString("origin_url");
                                                break;
                                            }

                                            if (!StringUtils.isBlank(originUrl)){
                                                promise.complete(originUrl);
                                            }else{
                                                promise.fail("not exist");
                                            }
                                        }
                                        conn.close();
                                    });
                        }else{
                            logger.error("异常：{}", ar1.cause().getMessage());
                            promise.fail(ar1.cause());
                        }
                    });
                } else {
                    promise.complete(url);
                }

            } else {
                promise.fail("not exist");
            }
        }, promise.future().onComplete(resultHandler));

    }

    private String hash(String value){
        return Hashing.murmur3_32().hashString(value, StandardCharsets.UTF_8).toString();
    }
}
