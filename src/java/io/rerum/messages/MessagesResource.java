/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.rerum.messages;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;

/**
 * REST Web Service
 *
 * @author cubap
 */
@Path("messages")
public class MessagesResource {

    @Context
    private UriInfo context;

    /**
     * Creates a new instance of MessagesResource
     */
    public MessagesResource() {
    }

    /**
     * Retrieves representation of an instance of io.rerum.messages.MessagesResource
     * @return an instance of java.lang.String
     * @throws java.lang.Exception
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getJson() throws Exception {
        return getMessages();
    }

    /**
     * PUT method for updating or creating an instance of MessagesResource
     * @param content representation for the resource
     */
    @PUT
    @Consumes(MediaType.APPLICATION_XML)
    public void putXml(String content) {
    }
        private String getMessages() throws Exception {
        URL noteUrl = new URL("https://rerum-inbox.firebaseio.com/messages/.json");
        HttpURLConnection conn = (HttpURLConnection) noteUrl.openConnection();

        StringBuilder response;
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        System.out.println(response.toString());
        return response.toString();
    }

}
