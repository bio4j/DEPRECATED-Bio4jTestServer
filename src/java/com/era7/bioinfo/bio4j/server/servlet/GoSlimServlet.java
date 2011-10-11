/*
 * Copyright (C) 2010-2011  "Bio4j"
 *
 * This file is part of Bio4j
 *
 * Bio4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.era7.bioinfo.bio4j.server.servlet;

import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4j.server.util.Bio4jLogger;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.bio4jmodel.util.GoUtil;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
import com.era7.lib.bioinfoxml.go.GOSlimXML;
import com.era7.lib.bioinfoxml.go.SlimSetXML;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.jdom.Element;

/**
 * Returns a XML structure including the corresponding result for the GOSlim job specified as parameter.
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GoSlimServlet extends BasicServletNeo4j {

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {


        Response response = new Response();
        String method = request.getMethod();

        System.out.println("GoSlimServlet");
        
        String reqSt = request.toString();


        if (method.equals(RequestList.GO_SLIM_REQUEST)) {

            List<Element> proteinList = request.getParameters().getChild("proteins").getChildren(ProteinXML.TAG_NAME);
            ArrayList<ProteinXML> proteinArray = new ArrayList<ProteinXML>();
            for (Element elem : proteinList) {
                proteinArray.add(new ProteinXML((Element)elem.clone()));
            }

            SlimSetXML slimSet = new SlimSetXML(request.getParameters().getChild(SlimSetXML.TAG_NAME));

            GOSlimXML goSlim = GoUtil.getGoSlim(proteinArray, slimSet, manager, null);

            response.addChild(goSlim);
            response.setStatus(Response.SUCCESSFUL_RESPONSE);

            //logging request
            Bio4jLogger.log(Bio4jLogger.createLogRecord(hsr, reqSt, RequestList.GO_SLIM_REQUEST));

        } else {
            response.setError("There is no such method");
        }

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
