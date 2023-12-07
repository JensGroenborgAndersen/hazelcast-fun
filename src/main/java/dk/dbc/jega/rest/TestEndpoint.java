package dk.dbc.jega.rest;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.UUID;
import java.util.stream.Collectors;

@Stateless
@Path("")
public class TestEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestEndpoint.class);
    private IMap<Long, UUID> map;
    @Inject
    private HazelcastInstance instance;

    @PostConstruct
    public void init() {
        try {
            LOGGER.info("Got hazelcast instance " + instance);
            map = instance.getMap("test.repmap");
        } catch (Exception e) {
            LOGGER.error("AAAARGGHHH", e);
        }
    }

    @GET
    @Path("ping")
    @Produces(MediaType.TEXT_PLAIN)
    public Response ping() {
        return Response.ok("pong").build();
    }

    @GET
    @Path("test")
    @Produces(MediaType.TEXT_PLAIN)
    public Response test(@QueryParam("id") Long id) {
        try {
            if(id == null || !map.containsKey(id)) map.set(id == null ? System.currentTimeMillis() : id, UUID.randomUUID());
            String globals = "Globally:\n" + map.entrySet().stream().map(e -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n"));
            String locals = "Local keys:\n" + map.localKeySet().stream().map(Object::toString).collect(Collectors.joining("\n"));
            return Response.ok(globals + "\n\n" + locals).build();
        } catch (Exception e) {
            return Response.accepted(e).status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("facts")
    @Produces(MediaType.TEXT_PLAIN)
    public Response info() {
        return Response.ok(instance.getCluster().getMembers().toString()).build();
    }
}
