/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.era7.bioinfo.bio4j.server.servlet;


import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4j.server.util.FileUploadUtilities;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetGoAnnotationChartXMLServlet extends HttpServlet {

    @Override
    public void init() {
    }

    @Override
    public void doPost(javax.servlet.http.HttpServletRequest request,
            javax.servlet.http.HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        //System.out.println("doPost !");
        servletLogic(request, response);

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        //System.out.println("doGet !");
        servletLogic(request, response);


    }

    protected void servletLogic(HttpServletRequest request, HttpServletResponse resp)
            throws javax.servlet.ServletException, java.io.IOException {


        OutputStream out = resp.getOutputStream();

        String temp = request.getParameter(Request.TAG_NAME);

        try {

            Request myReq = new Request(temp);

            String method = myReq.getMethod();

            if (method.equals(RequestList.GET_GO_ANNOTATION_CHART_XML_REQUEST)) {

                boolean isMultipart = ServletFileUpload.isMultipartContent(request);

                if (!isMultipart) {
                    Response response = new Response();
                    response.setError("No file was uploaded");
                    out.write(response.toString().getBytes());
                } else {

                    FileItem fileItem = FileUploadUtilities.getFileItem(request);
                    InputStream uploadedStream = fileItem.getInputStream();
                    BufferedReader inBuff = new BufferedReader(new InputStreamReader(uploadedStream));


                    uploadedStream.close();


                    String responseSt = "";

                    resp.setContentType("text/html");

                    byte[] byteArray = responseSt.getBytes();
                    out.write(byteArray);
                    resp.setContentLength(byteArray.length);

                }

            } else {
                Response response = new Response();
                response.setError("There is no such method");
                out.write(response.toString().getBytes());

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
