/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.era7.bioinfo.bio4j.server.util;

import java.util.Iterator;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author Pablo Pareja Tobes <ppareja@era7.com>
 */
public class FileUploadUtilities {

    public static FileItem getFileItem(HttpServletRequest httpRequest) throws FileUploadException {
        FileItemFactory factory = new DiskFileItemFactory();
        ServletFileUpload upload = new ServletFileUpload(factory);
        List items = upload.parseRequest(httpRequest);
        Iterator iter = items.iterator();
        FileItem item = null;
        while (iter.hasNext()) {
            item = (FileItem) iter.next();
            if (!item.isFormField()) {
                return item;
            }
        }
        return item;
    }
}
