package org.mokesoft.art.shortlink.service;

import org.mokesoft.art.shortlink.form.ShortLinkForm;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@ProxyGen
public interface ShortLinkAsyncService {
    String ADDRESS = ShortLinkAsyncService.class.getName();

    void save(ShortLinkForm form, Handler<AsyncResult<String>> resultHandler);

    void get(String url, Handler<AsyncResult<String>> resultHandler) ;
}
