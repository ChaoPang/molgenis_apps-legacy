package org.molgenis.compute.scriptserver;

import org.molgenis.compute.monitor.LoggingReaderSsh;
import org.molgenis.compute.pipelinemodel.FileToSaveRemotely;
import org.molgenis.compute.pipelinemodel.Pipeline;
import org.molgenis.compute.pipelinemodel.Script;
import org.molgenis.util.Ssh;
import org.molgenis.util.SshResult;

import java.io.IOException;


//thread class for a pipeline; every pipeline runs in the separated thread
public abstract class PipelineThreadSsh extends PipelineThread
{
    protected Ssh ssh = null;

    public PipelineThreadSsh(Pipeline pipeline)
    {
        this.pipeline = pipeline;

        startMonitor();
        //to monitor pipeline execution from outside
        pipeline.setMonitor(monitor);

        //set pipeline for demo purposes
        monitor.setPipeline(pipeline);

        monitor.setLogFile(pipeline.getPipelinelogpath());
        startClusterSsh();
    }

    protected abstract void startMonitor();
    protected abstract void startClusterSsh();

    @Override
    protected boolean submitScript(Script script)
    {
        String output = null, error = null;
        String remoteName = script.getRemoteDir() + script.getRemotename();

        for(int i = 0; i < script.getNumberFileToSaveRemotely(); i++)
        {
            FileToSaveRemotely aFile = script.getFileToSaveRemotely(i);
            try
            {
                ssh.uploadStringToFile(new String(aFile.getFileData()), aFile.getRemoteName(), script.getRemoteDir());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        try
        {
            ssh.uploadStringToFile(new String(script.getScriptData()), script.getRemotename(), script.getRemoteDir());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        String command = script.getSubmitCommand();

        SshResult result = null;
        try
        {
            result = ssh.executeCommand(command);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        output = result.getStdOut();
        error = result.getStdErr();

        System.out.println("Output: " + output);
        if (error == null || "".equalsIgnoreCase(error)) {
        	System.out.println("Error: none!");
        } else {
        	System.out.println("Error: " + error);
        }

        if (error.toCharArray().length > 0 || output.toCharArray().length == 0)
            return true;
        return false;
    }
}
