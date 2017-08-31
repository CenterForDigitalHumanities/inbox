/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package io.rerum.messages;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

/**
 * REST Web Service
 *
 * @author cubap
 */
@Path("messages")
public class MessagesResource {
    
    private final String CONTEXT = "http://www.w3.org/ns/ldp";
    private final String ID_ROOT = "http://inbox.rerum.io/messages";
    private final String TYPE = "ldp:Container";

    @DefaultValue("") @QueryParam("target") String TARGET;
    @DefaultValue("") @QueryParam("type") String Q_TYPE;
    @DefaultValue("") @QueryParam("motivation") String MOTIVATION;
    
    @Context Request request;
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
        
        JsonObject messages = getMessages();
        return messages.toString();
    }

    /**
     * PUT method for updating or creating an instance of MessagesResource
     * @return 
     */
    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    public Response putPending() {
        return Response.status(HttpURLConnection.HTTP_BAD_METHOD).entity("PUT is not implemented for this inbox.").build();
    }
    
    /**
     *
     * @param content
     * @return
     */
    @POST
    @Consumes(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public String postJson(String content) {
        JsonObject announcement;
        try (JsonReader reader = Json.createReader(new StringReader(content))) {
            announcement = reader.readObject();
        }
        // Simple check for parts
            Logger.getLogger(Message.class.getName()).log(Level.INFO, announcement.toString());
        // ERRORS
        if (announcement.containsKey("@id")) {
            // There should not be one already; these are new annoucements.
            Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("POST is for new annoucements only.").build();
            return "{\"error\":\"Property '@id' indicates this is not a new annoucement.\"}";
        }
        if (!announcement.containsKey("motivation")) {
            // This should always exist.
            Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("Missing 'motivation' property.").build();
            return "{\"error\":\"Annoucements without 'motivation' are not allowed on this server.\"}";
        }

        //NORMALIZATIONS       
        if (!announcement.containsKey("@context")) {
            // Put in a default, please.
            announcement = new Message().generate(announcement, "@context", CONTEXT);
        }
        
        // add timestamp
        Date time = new Date();
            announcement = new Message().generate(announcement, "published", time.toString());

        try {
            announcement = postMessage(announcement);
        } catch (Exception ex) {
            Logger.getLogger(Message.class.getName()).log(Level.SEVERE, null, ex);
        }
        return announcement.toString();
    }

    private JsonObject postMessage(JsonObject content) throws Exception {
        URL noteUrl = new URL("https://rerum-inbox.firebaseio.com/messages.json");
        HttpURLConnection conn = (HttpURLConnection) noteUrl.openConnection();
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        
conn.setDoOutput(true);
conn.setDoInput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(content.toString());
        wr.close();
        
        StringBuilder response;
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(conn.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } 
        JsonReader reader = Json.createReader(new StringReader(response.toString()));
        JsonObject source = reader.readObject();
        Logger.getLogger(Message.class.getName()).log(Level.INFO, "Created: "+source.getJsonString("name").toString());
        reader.close();
        return source;
    }
    
    /**
     *
     * @return
     */
    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    public Response deletePending() {
            return Response.status(HttpURLConnection.HTTP_BAD_METHOD).entity("DELETE is not implemented for this inbox.").build();
    }
       
    private JsonObject getMessages() throws Exception {
    URL noteUrl = new URL("https://rerum-inbox.firebaseio.com/messages.json"+buildQuery());
    HttpURLConnection conn = (HttpURLConnection) noteUrl.openConnection();
    InputStreamReader isr = new InputStreamReader(conn.getInputStream(),"UTF-8");
    JsonReader reader = Json.createReader(isr);
    JsonArray messages = readMessages(reader);
    return formatList(messages);
    }
        
    private JsonArray readMessages(JsonReader reader) throws Exception {
            JsonArrayBuilder b = Json.createArrayBuilder();
            JsonObject obj;
                    try{
            obj = reader.readObject();
            } finally {
                reader.close();
            }
            obj.entrySet().forEach(e -> 
            {
                Message msg=new Message();
                JsonObject m;
                m = msg.generate(e.getValue().toString(), "@id", "http://inbox.rerum.io/id/" + e.getKey() + "");
                if(MOTIVATION.length()==0 || MOTIVATION.contains(m.getJsonString("motivation").toString())){
                    b.add(m);
                }
                });
            JsonArray messages = b.build();
            return messages;
 }
    
    private JsonObject formatList(JsonArray arr){
        JsonObject response;
        JsonObjectBuilder b = Json.createObjectBuilder();
        b.add("@context", CONTEXT);
        b.add("@type", TYPE);
        b.add("@id", ID_ROOT+"?target="+TARGET);
        b.add("contains",arr);
        response = b.build();
        return response;
    }
    
    private String buildQuery(){
        String q = "";
        if(!TARGET.isEmpty() || !Q_TYPE.isEmpty() || !MOTIVATION.isEmpty()){
            q = "?";
        }
        if(Q_TYPE.length()>0){
            System.out.println("Type length:"+Q_TYPE.length());
            // What the hell, !.isEmpty() with String.length() == 0 is succeeding?
            q = q+"orderBy=\"type\"&equalTo=\""+Q_TYPE+"\"";
        }
//        if(MOTIVATION.length()>0){
//            q = q+"&equalTo=\""+MOTIVATION+"\"";
//        }
        if(TARGET.length()>0){
            q = q+"orderBy=\"target\"&equalTo=\""+TARGET+"\"";
        }
        return q;
    }

}
