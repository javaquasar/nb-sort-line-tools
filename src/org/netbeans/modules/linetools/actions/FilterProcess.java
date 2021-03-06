/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.linetools.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.openide.ErrorManager;

/**
 *
 * @author Sandip V. Chitale (Sandip.Chitale@Sun.Com)
 */
public class FilterProcess {
    
    private String[] filterCommand;
    private int expectedNumberOfOutputLines;
    
    private Process filterProcess;
    
    private PrintWriter printWriter;
    
    private List<String> filterProcessStdOut;
    private List<String> filterProcessStdErr;
    
    public FilterProcess(String[] filterCommand) {
        this(filterCommand, 100);
    }
    
    public FilterProcess(String[] filterCommand, int expectedNumberOfOutputLines) {
        this.filterCommand = filterCommand;
        this.expectedNumberOfOutputLines = expectedNumberOfOutputLines;
    }
    
    public PrintWriter exec() throws IOException {
        // Run the filter process
        filterProcess = Runtime.getRuntime().exec(filterCommand);
        
        // Setup STDOUT Reading
        filterProcessStdOut = new ArrayList<String>();
        Thread filterProcessStdOutReader = new Thread(
                new InputStreamReaderThread(filterProcess.getInputStream(),
                    filterProcessStdOut),
                    filterCommand[0] + ":STDOUT Reader"); // NOI18N
        filterProcessStdOutReader.start();
        
        // Setup STDERR Reading
        filterProcessStdErr = new ArrayList<String>(expectedNumberOfOutputLines);
        Thread filterProcessStdErrReader = new Thread(
                new InputStreamReaderThread(filterProcess.getErrorStream(),
                    filterProcessStdErr),
                    filterCommand[0] + ":STDERR Reader"); // NOI18N
        filterProcessStdErrReader.start();
        
        printWriter = new PrintWriter(filterProcess.getOutputStream());
        
        return printWriter;
    }
       
    public int waitFor() {
        if (filterProcess != null) {
            int exitStatus;
            try {
                return filterProcess.waitFor();
            } catch (InterruptedException ex) {
                ErrorManager.getDefault().notify(ErrorManager.USER, ex);
            }
        }
        return -1;
    }
    
    public String[] getStdOutOutput() {
        if (filterProcessStdOut != null) {
            return (String[]) filterProcessStdOut.toArray(new String[0]);
        }
        return null;
    }
    
    public String[] getStdErrOutput() {
        if (filterProcessStdErr != null) {
            return (String[]) filterProcessStdErr.toArray(new String[0]);
        }
        return null;
    }
    
    public void destroy() {
        if (filterProcess != null) {
            filterProcess.destroy();
            filterProcess = null;
            filterProcessStdOut = null;
            filterProcessStdErr = null;
        }
    }
    
    static class InputStreamReaderThread implements Runnable {
        private InputStream is;
        private List<String> output;
        
        InputStreamReaderThread(InputStream is, List<String> output) {
            this.is = is;
            this.output = output;
        }
        
        public void run() {
            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                String line;
                while ((line = br.readLine()) != null) {
                    output.add(line);
                }
            } catch (IOException ioe) {
                ErrorManager.getDefault().notify(ioe);
            }
        }
    }
}
