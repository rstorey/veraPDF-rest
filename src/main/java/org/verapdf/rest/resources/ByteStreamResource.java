package org.verapdf.rest.resources;

import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.openpreservation.bytestreams.ByteStreamId;
import org.openpreservation.bytestreams.ByteStreams;


/**
 * The REST resource definition for byte stream identification services, these
 * are JERSEY REST services and it's the annotations that perform the magic of
 * handling content types and serialisation.
 * 
 * @author <a href="mailto:carl@openpreservation.org">Carl Wilson</a>.</p>
 */
public class ByteStreamResource {
    /**
     * Default public constructor required by Jersey / Dropwizard
     */
    ByteStreamResource() {
        /* Intentionally blank */
    }

    /**
     * @param uploadedInputStream
     *            InputStream for the uploaded file
     * @param contentDispositionHeader
     *            extra info about the uploaded file, currently unused.
     * @return the {@link org.openpreservation.bytestreams.ByteStreamId} of
     *         the uploaded file's byte stream serialised according to requested
     *         content type.
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML })
    public static ByteStreamId getSha1(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") final FormDataContentDisposition contentDispositionHeader) {
            return calculateSha1(uploadedInputStream);
    }

    /**
     * @param inFile
     *            byte[] of the incoming file
     * @param httpHeaders
     *          the {@link javax.ws.rs.core.Context} with {@link javax.ws.rs.core.HttpHeaders}
     * @return the {@link org.openpreservation.bytestreams.ByteStreamId} of
     *         the uploaded file's byte stream serialised according to requested
     *         content type.
     */
    @PUT
    @Consumes(MediaType.WILDCARD)
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML })
    public static ByteStreamId getSha1Put(
           @Context HttpHeaders httpHeaders,
           byte [] inFile) {
        return calculateSha1(new ByteArrayInputStream(inFile));
    }


    /**
     * @return the {@link org.openpreservation.bytestreams.ByteStreamId} of
     *         an empty (0 byte) byte stream serialised according to requested
     *         content type.
     */
    @GET
    @Path("/null")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
            MediaType.TEXT_XML })
    public static ByteStreamId getEmptySha1() {
        return ByteStreams.nullByteStreamId();
    }

    private static ByteStreamId calculateSha1(InputStream uploadedInputStream) {
        try {
            ByteStreamId id = ByteStreams.idFromStream(uploadedInputStream);
            uploadedInputStream.close();
            return id;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ByteStreams.nullByteStreamId();
    }

}
