/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.bio4j.server.servlet;

import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4j.server.util.Bio4jLogger;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.GoUtil;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
import com.era7.lib.bioinfoxml.ProteinXML;
import com.era7.lib.bioinfoxml.go.GoAnnotationXML;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.jdom.Element;

/**
 *
 * @author ppareja
 */
public class GoAnnotationServlet extends BasicServletNeo4j {

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {


        //Logger logger = Logger.getLogger("Logger");
        //logger.log(Level.SEVERE, request.toString());

        System.out.println("GoAnnotationServlet");

        Response response = new Response();
        String method = request.getMethod();

        if (method.equals(RequestList.GO_ANNOTATION_REQUEST)) {

            Element proteinsXml = request.getParameters().getChild("proteins");

            ArrayList<ProteinXML> array = new ArrayList<ProteinXML>();
            List<Element> list = proteinsXml.getChildren(ProteinXML.TAG_NAME);
            for (Element elem : list) {
                array.add(new ProteinXML((Element)elem.clone()));
            }

            GoAnnotationXML goAnnotationXML = GoUtil.getGoAnnotation(array, manager);

            response.addChild(goAnnotationXML);
            response.setStatus(Response.SUCCESSFUL_RESPONSE);

            //logging request
            Bio4jLogger.log(Bio4jLogger.createLogRecord(hsr, request.toString(), RequestList.GO_ANNOTATION_REQUEST));

        } else {
            response.setError("There is no such method");
        }

        //logger.log(Level.SEVERE, response.toString());

        return response;
    }

    @Override
    protected void logSuccessfulOperation(Request rqst, Response rspns, Bio4jManager manager,
            BasicSession session) {
    }

    @Override
    protected void logErrorResponseOperation(Request rqst, Response rspns, Bio4jManager manager,
            BasicSession session) {
    }

    @Override
    protected void logErrorExceptionOperation(Request rqst, Response rspns, Throwable thrwbl,
            Bio4jManager manager) {
    }

    @Override
    protected void noSession(Request request) {
    }
    @Override
    protected boolean checkPermissions(ArrayList<?> al, Request rqst) {
        return true;
    }

    @Override
    protected boolean defineCheckSessionFlag() {       return false;   }
    @Override
    protected boolean defineCheckPermissionsFlag() {   return false;}
    @Override
    protected boolean defineLoggableFlag() {    return false; }
    @Override
    protected boolean defineLoggableErrorsFlag() {  return false; }
    @Override
    protected boolean defineUtf8CharacterEncodingRequest() {    return false; }
    @Override
    protected void initServlet() { }

    @Override
    protected String defineNeo4jDatabaseFolder() {
        return CommonData.DATABASE_FOLDER;
    }
}
