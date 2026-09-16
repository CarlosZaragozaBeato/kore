package com.zensyra.suunto.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import java.util.concurrent.atomic.AtomicInteger;

@Path("/test/suunto")
public class RetryTestResource {

    private final AtomicInteger attempts = new AtomicInteger();

    @GET
    @Path("/429")
    public Response tooManyRequests() {
        int attempt = attempts.incrementAndGet();

        System.out.println("TEST 429 attempt #" + attempt);

        return Response.status(429).build();
    }

    @GET
    @Path("/401")
    public Response unauthorized() {
        int attempt = attempts.incrementAndGet();

        System.out.println("TEST 401 attempt #" + attempt);

        return Response.status(401).build();
    }

    @GET
    @Path("/500")
    public Response serverError() {
        int attempt = attempts.incrementAndGet();

        System.out.println("TEST 500 attempt #" + attempt);

        return Response.status(500).build();
    }

    @GET
    @Path("/reset")
    public Response reset() {
        attempts.set(0);
        return Response.ok().build();
    }

    @GET
    @Path("/attempts")
    public Response attempts() {
        return Response.ok(attempts.get()).build();
    }
}