/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.era7.bioinfo.bio4j.server.servlet;

import com.era7.bioinfo.bio4j.server.CommonData;
import com.era7.bioinfo.bio4jmodel.nodes.ProteinNode;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
import com.era7.bioinfo.bio4j.server.RequestList;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.lib.bioinfoxml.ProteinXML;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.jdom.Element;
import org.neo4j.graphdb.Node;
//import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.index.Index;

/**
 *
 * @author ppareja
 */
public class DownloadProteinMultifastaServlet extends BasicServletNeo4j{

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {

        Response response = new Response();

        if(request.getMethod().equals(RequestList.DOWNLOAD_PROTEIN_MULTIFASTA_REQUEST)){

            List<Element> proteins = request.asJDomElement().getChildren(ProteinXML.TAG_NAME);

//            int maxProteinsPerTxn = 1000;
//            int counter = 0;
//            Transaction txn = manager.beginTransaction();
            //IndexService indexService = manager.getIndexService();

            Index<Node> proteinAccessionIndex = manager.getProteinAccessionIndex();

            try{
                for (Element element : proteins) {
                    ProteinXML protein = new ProteinXML(element);


                    ProteinNode proteinNode = new ProteinNode(proteinAccessionIndex.get(ProteinNode.PROTEIN_ACCESSION_INDEX, protein.getId()).getSingle());

//                    counter++;
//                    if(counter > maxProteinsPerTxn){
//                        counter = 0;
//                        txn.success();
//                        txn.finish();
//                        txn = manager.beginTransaction();
//                    }
                }
//                txn.success();
            }catch(Exception e){
//                txn.failure();
                response.setError(e.getMessage());
            }finally{
//                txn.finish();
            }            


            response.setStatus(Response.SUCCESSFUL_RESPONSE);

        }else{
            response.setError("No such method");
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
