package com.era7.bioinfo.bio4j.server.servlet;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4j.server.util.Bio4jLogger;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.GoUtil;
import com.era7.lib.bioinfoxml.ProteinXML;
import com.era7.lib.bioinfoxml.go.GoAnnotationXML;
import com.era7.lib.communication.xml.Request;
import javax.servlet.http.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;

public class DownloadGoAnnotationServlet extends HttpServlet {

    @Override
    public void init() {
    }

    @Override
    public void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        servletLogic(request, response);

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        servletLogic(request, response);


    }

    private void servletLogic(HttpServletRequest request, HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {


        OutputStream out = response.getOutputStream();

        try {

            Request myReq = new Request(request.getParameter(Request.TAG_NAME));

            //System.out.println("myReq = " + myReq);


            if (myReq.getMethod().equals(RequestList.DOWNLOAD_GO_ANNOTATION_REQUEST)) {

                String fileName = myReq.getParameters().getChildText("file_name");

                Element proteinsXml = myReq.getParameters().getChild("proteins");

                ArrayList<ProteinXML> array = new ArrayList<ProteinXML>();
                List<Element> list = proteinsXml.getChildren(ProteinXML.TAG_NAME);
                for (Element elem : list) {
                    array.add(new ProteinXML(elem));
                }


                GoAnnotationXML goAnnotationXML = GoUtil.getGoAnnotation(array, new Bio4jManager(CommonData.DATABASE_FOLDER));


                response.setContentType("application/x-download");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xml");


                byte[] byteArray = goAnnotationXML.toString().getBytes();

                out.write(byteArray);
                response.setContentLength(byteArray.length);
                

            } else {
                out.write("There is no such method".getBytes());
            }
            
            //logging request
            Bio4jLogger.log(Bio4jLogger.createLogRecord(request, myReq.toString(), RequestList.DOWNLOAD_GO_ANNOTATION_REQUEST));



        } catch (Exception e) {
            out.write("Error...".getBytes());
            out.write(e.getStackTrace()[0].toString().getBytes());
        }

        out.flush();
        out.close();
    }
}


