package network.bitmesh.cloudserver.Servlets;

import network.bitmesh.cloudserver.Bitcoin.LockedTransactionBroadcasterTimer;
import network.bitmesh.cloudserver.Utils.CommonResponses;

import org.bitcoinj.core.*;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Date;
import javax.servlet.*;
import javax.servlet.http.*;

public class TransactionServlet extends HttpServlet
{
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TransactionServlet.class.getName());

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        // Allow cross origin requests
        CommonResponses.allowCrossOrigin(resp);

        byte[] transaction = new byte[req.getContentLength()];

        InputStream transactionInputStream = req.getInputStream();

        long startTime = System.nanoTime();

        int alreadyRead = 0;
        while(req.getContentLength() > alreadyRead && !CommonResponses.shouldTimeOut(startTime))
        {
            alreadyRead += transactionInputStream.read(transaction, alreadyRead, transactionInputStream.available());
        }

        Transaction trans = null;
        PrintWriter out = resp.getWriter();
        boolean error = false;

        try
        {
            log.info("Received transaction post - TX: " + trans.getHashAsString());
            trans = new Transaction(MainNetParams.get(), transaction);
            trans.verify();
        }
        catch(Exception e)
        {
            try
            {
                log.info("Parsing as TestNet");
                trans = new Transaction(TestNet3Params.get(), transaction);
                trans.verify();
            }
            catch (Exception e1)
            {
                log.error("Error parsing transaction.");
                resp.setStatus(resp.SC_BAD_REQUEST);
                out.println("<html><body><p>Failed to parse transaction.</p></body></html>");
                e.printStackTrace();
                error = true;
            }
            finally
            {
                if(out != null)
                    out.close();
                // Don't schedule the transaction if it couldn't be parsed
                if(error == true)
                    return;
            }
        }

        LockedTransactionBroadcasterTimer.getInstance().scheduleTransactionBroadcast(trans);
        resp.setStatus(resp.SC_OK);
        log.info("Transaction will be posted at: " + new Date(trans.getLockTime() * 1000).toString());
        return;
    }

    /**
     * jQuery asks for Options before it does the ajax request. We have to add
     * the CORS headers to this request. Probably should be using a filter for this.
     */
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        CommonResponses.allowCrossOrigin(resp);
        resp.setStatus(resp.SC_OK);
    }
}
