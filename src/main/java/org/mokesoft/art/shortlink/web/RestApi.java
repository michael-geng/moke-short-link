package org.mokesoft.art.shortlink.web;

import org.mokesoft.art.shortlink.common.HttpUtils;
import org.mokesoft.art.shortlink.common.ResponseResult;
import org.mokesoft.art.shortlink.handler.ShortLinkHandler;
import org.mokesoft.art.shortlink.service.ShortLinkAsyncService;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.serviceproxy.ServiceProxyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import static java.net.HttpURLConnection.HTTP_OK;


@Component
public class RestApi extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(RestApi.class);

    private ShortLinkAsyncService urlAsyncService;

    @Override
    public void start() throws Exception {

        urlAsyncService = new ServiceProxyBuilder(vertx).setAddress(ShortLinkAsyncService.ADDRESS).build(ShortLinkAsyncService.class);


        Router router = Router.router(vertx);

        router.route().handler(BodyHandler.create()).produces("application/json").handler(LoggerHandler.create());

        router.get("/nginx.html").handler(routingContext->{
            routingContext.response().end("true");
        });

        router.get("/:hash").handler(routingContext -> ShortLinkHandler.get(routingContext, urlAsyncService));
        router.post("/admin/addShortLink").handler(routingContext -> ShortLinkHandler.shortLink(routingContext, urlAsyncService));


        router.route().failureHandler(rc -> {
            HttpUtils.fireResponse(rc.response(), HTTP_OK, ResponseResult.build(1, rc.failure().getMessage()));
        });

        vertx.createHttpServer().requestHandler(router).listen(8080, listen -> {
            if (listen.succeeded()) {
                logger.info("RestApi started");

            } else {
                logger.error("RestApi start error");
            }
        });


    }

}