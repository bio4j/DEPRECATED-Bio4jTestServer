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
import com.era7.lib.bioinfoxml.go.GOSlimXML;
import com.era7.lib.bioinfoxml.go.SlimSetXML;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.communication.xml.Request;
import javax.servlet.http.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import org.jdom.Element;


public class DownloadGoSlimServlet extends HttpServlet {

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
    public void doGet(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {
        //System.out.println("doGet !");
        servletLogic(request, response);


    }

    private void servletLogic(javax.servlet.http.HttpServletRequest request, javax.servlet.http.HttpServletResponse response)
            throws javax.servlet.ServletException, java.io.IOException {


        OutputStream out = response.getOutputStream();

        try {

            String temp = request.getParameter(Request.TAG_NAME);
            Request myReq = new Request(temp);

            //System.out.println("myReq = " + myReq);
            
            String reqSt = myReq.toString();

            if (myReq.getMethod().equals(RequestList.DOWNLOAD_GO_SLIM_REQUEST)) {

                String fileName = myReq.getParameters().getChildText("file_name");
                //boolean countGos = Boolean.parseBoolean(myReq.getParameters().getChildText("count_gos"));

                List<Element> proteinList = myReq.getParameters().getChild("proteins").getChildren(ProteinXML.TAG_NAME);
                ArrayList<ProteinXML> proteinArray = new ArrayList<ProteinXML>();
                for (Element elem : proteinList) {
                    proteinArray.add(new ProteinXML(elem));
                }

                SlimSetXML slimSet = new SlimSetXML(myReq.getParameters().getChild(SlimSetXML.TAG_NAME));
                //System.out.println("slimSet = " + slimSet);

                GOSlimXML goSlim = GoUtil.getGoSlim(proteinArray, slimSet, new Bio4jManager(CommonData.DATABASE_FOLDER), null);

                response.setContentType("application/x-download");
                response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".xml");

                //System.out.println("goSlim = " + goSlim);

                byte[] byteArray = goSlim.toString().getBytes();

                out.write(byteArray);
                response.setContentLength(byteArray.length);


            } else {
                out.write("There is no such method".getBytes());
            }
            
            //logging request
            Bio4jLogger.log(Bio4jLogger.createLogRecord(request, reqSt, RequestList.DOWNLOAD_GO_SLIM_REQUEST));



        } catch (Exception e) {
            out.write("There was an error...".getBytes());
            out.write(e.getStackTrace()[0].toString().getBytes());
        }

        out.flush();
        out.close();
    }
}


