/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.rerum.messages;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

/**
 * REST Web Service
 *
 * @author cubap
 */
@Path("id/{noteId}")
public class Message {

    @Context
    private UriInfo context;

    @PathParam("noteId")
    String noteId;

    /**
     * Creates a new instance of Message
     */
    public Message() {
    }

    /**
     * Retrieves representation of an instance of io.rerum.messages.Message
     * proxy to firebase.io. The request looks like:
     * https://rerum-inbox.firebaseio.com/messages/-KopRqrP9czBnzJtieaE.json
     * is https://rerum-inbox.firebaseio.com/messages/{noteId}.json
     *
     * The response is like: { "@type":"Announce",
     * "motivation":"iiif:supplement:range",
     * "object":"http://scta.info/iiif/wodehamordinatio/sorb193/ranges/toc/wrapper",
     * "source":"http://scta.info", "actor":{ "@id":
     * "https://scta.info/#identity, "label": "SCTA" },
     * "target":"https://nubis.univ-paris1.fr/items/presentation/4853/manifest",
     * "published":"2017-03-18 13:12:46 UTC" }
     *
     * and needs an @id and @context constructed for it, if not present
     * @return an instance of java.lang.String
     */
    @GET
    
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public String getJson() throws Exception {

        String noteObjStr = getMessage();
        
        return noteObjStr;
        }
    /**
     * PUT method for updating or creating an instance of Message
     *
     * @param content representation for the resource
     */
    @PUT
    @Consumes(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public String putJson(String content) {
        // TODO: PUT content
        return content;
    }

    private String getMessage() throws Exception {
        URL noteUrl = new URL("https://rerum-inbox.firebaseio.com/messages/" + noteId+".json");
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
        System.out.println(response.toString() + "| https://rerum-inbox.firebaseio.com/messages/" + noteId+".json");
        return response.toString();
    }
}
