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
import com.era7.bioinfo.bio4jmodel.nodes.InterproNode;
import com.era7.bioinfo.bio4jmodel.nodes.KeywordNode;
import com.era7.bioinfo.bio4jmodel.nodes.ProteinNode;
import com.era7.bioinfo.bio4jmodel.relationships.comment.DomainCommentRel;
import com.era7.bioinfo.bio4jmodel.relationships.comment.FunctionCommentRel;
import com.era7.bioinfo.bio4jmodel.relationships.comment.PathwayCommentRel;
import com.era7.bioinfo.bio4jmodel.util.Bio4jManager;
import com.era7.bioinfo.servletlibraryneo4j.servlet.BasicServletNeo4j;
import com.era7.lib.bioinfoxml.uniprot.CommentXML;
import com.era7.lib.bioinfoxml.uniprot.InterproXML;
import com.era7.lib.bioinfoxml.uniprot.KeywordXML;
import com.era7.lib.bioinfoxml.uniprot.ProteinXML;
import com.era7.lib.communication.model.BasicSession;
import com.era7.lib.communication.xml.Request;
import com.era7.lib.communication.xml.Response;
import com.era7.lib.era7xmlapi.model.XMLElement;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.jdom.Element;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

/**
 * Completes the information of all proteins passed as parameter including info like:
 * names, interpro, keywords, pathway, organism, sequences...
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class GetProteinInfoServlet extends BasicServletNeo4j {

    @Override
    protected Response processRequest(Request request, BasicSession session, Bio4jManager manager,
            HttpServletRequest hsr) throws Throwable {


        Response response = new Response();
        String method = request.getMethod();

        if (method.equals(RequestList.GET_PROTEIN_INFO_REQUEST)) {

            Element proteinsXml = request.getParameters().getChild("proteins");

            ArrayList<ProteinXML> array = new ArrayList<ProteinXML>();
            List<Element> list = proteinsXml.getChildren(ProteinXML.TAG_NAME);
            for (Element elem : list) {
                elem.detach();
                array.add(new ProteinXML(elem));
            }

            XMLElement proteinsResult = new XMLElement(new Element("proteins"));

            Index<Node> proteinIndex = manager.getProteinAccessionIndex();

            for (ProteinXML protein : array) {
                IndexHits<Node> proteinHits = proteinIndex.get(ProteinNode.PROTEIN_ACCESSION_INDEX, protein.getId());
                if(proteinHits.hasNext()){
                    
                    ProteinNode protNode = new ProteinNode(proteinHits.getSingle());   
                    ProteinXML proteinXML = new ProteinXML();
                    
                    //--------------setting protein data-----------
                    proteinXML.setId(protNode.getAccession());
                    proteinXML.setLength(protNode.getLength());
                    proteinXML.setFullName(protNode.getFullName());
                    proteinXML.setProteinName(protNode.getName());
                    proteinXML.setShortName(protNode.getShortName());
                    proteinXML.setSequence(protNode.getSequence());
                    proteinXML.setOrganism(protNode.getOrganism().getScientificName());
                    
                    //---keywords---
                    for (KeywordNode keyword : protNode.getKeywords()) {
                        KeywordXML kXML = new KeywordXML();
                        kXML.setId(keyword.getId());
                        kXML.setKeywordName(keyword.getName());
                        proteinXML.addKeyword(kXML);
                    }
                    
                    //----interpro----
                    for(InterproNode interproNode : protNode.getInterpro()){
                        InterproXML interproXML = new InterproXML();
                        interproXML.setId(interproNode.getId());
                        interproXML.setInterproName(protNode.getName());
                        proteinXML.addInterpro(interproXML);
                    }
                    
                    //----function comment-------
                    for (FunctionCommentRel tempRel : protNode.getFunctionComment()) {
                        CommentXML commentXML = new CommentXML();
                        commentXML.setType(FunctionCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                        commentXML.setCommentText(tempRel.getText());
                        proteinXML.addComment(commentXML);
                    }
                    
                    //----pathway comment-------
                    for (PathwayCommentRel tempRel : protNode.getPathwayComment()) {
                        CommentXML commentXML = new CommentXML();
                        commentXML.setType(PathwayCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                        commentXML.setCommentText(tempRel.getText());
                        proteinXML.addComment(commentXML);
                    }
                    
                    //----domain comment-------
                    for (DomainCommentRel tempRel : protNode.getDomainComment()) {
                        CommentXML commentXML = new CommentXML();
                        commentXML.setType(DomainCommentRel.UNIPROT_ATTRIBUTE_TYPE_VALUE);
                        commentXML.setCommentText(tempRel.getText());
                        proteinXML.addComment(commentXML);
                    }
                    
                    
                    proteinsResult.addChild(proteinXML);
                }
                
            }


            response.addChild(proteinsResult);
            response.setStatus(Response.SUCCESSFUL_RESPONSE);

        } else {
            response.setError("There is no such method");
        }


        return response;
    }

    @Override
    protected void logSuccessfulOperation(Request rqst, Response rspns, Bio4jManager manager,
            BasicSession session) {    }
    @Override
    protected void logErrorResponseOperation(Request rqst, Response rspns, Bio4jManager manager,
            BasicSession session) {    }
    @Override
    protected void logErrorExceptionOperation(Request rqst, Response rspns, Throwable thrwbl,
            Bio4jManager manager) {   }
    @Override
    protected void noSession(Request request) {   }
    @Override
    protected boolean checkPermissions(ArrayList<?> al, Request rqst) {        return true;    }
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
