package com.itsmiki.mydata;

import android.os.AsyncTask;
import android.content.Context;
import android.widget.Toast;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import static com.itsmiki.mydata.MainActivity.ID_CODE;
import static com.itsmiki.mydata.MainActivity.SERVER_HOST;
import static com.itsmiki.mydata.MainActivity.SERVER_USERNAME;
import static com.itsmiki.mydata.MainActivity.DESTINATION_DIRECTORY;


import java.io.File;
import java.io.PrintWriter;

public class SFTPconnection extends AsyncTask<Void, Void, int[]> {
    private Context ctx;

    public SFTPconnection(Context _ctx){
        ctx = _ctx;
    }

    @Override
    protected int[] doInBackground(Void... voids) {

    JSch jsch = new JSch();
        Session session = null;
        int [] files_sent = {0, 0, 0};
        String gps_file = String.valueOf(new File(ctx.getExternalFilesDir(null), ID_CODE + "_gps.txt"));
        String AppU_file = String.valueOf(new File(ctx.getExternalFilesDir(null), ID_CODE + "_AppU.txt"));
        String survey_file = String.valueOf(new File(ctx.getExternalFilesDir(null), ID_CODE + "_survey.txt"));

        try{
            // set up session
            session = jsch.getSession(SERVER_USERNAME,SERVER_HOST);
            // use private key instead of username/password
            session.setConfig(
                    "PreferredAuthentications",
                    "publickey,gssapi-with-mic,keyboard-interactive,password");
            jsch.addIdentity(String.valueOf(new File(ctx.getExternalFilesDir(null), "Identity/identity_mhas")));
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();

            // copy local files to host.
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();


            try {
                channelSftp.put(gps_file, DESTINATION_DIRECTORY);
                 files_sent[0] = 1;
                //overwrite contents with a blank file so that we don't duplicate
                PrintWriter pw = new PrintWriter(gps_file);
                pw.close();
            } catch (SftpException e) {
                e.printStackTrace();
            }
            try {
                channelSftp.put(AppU_file, DESTINATION_DIRECTORY);
                files_sent[1] = 1;
                //overwrite contents with a blank file
                PrintWriter pw = new PrintWriter(AppU_file);
                pw.close();
            } catch (SftpException e) {
                e.printStackTrace();
            }
            try{
                channelSftp.put(survey_file, DESTINATION_DIRECTORY);
                files_sent[2] = 1;
                //overwrite contents with a blank file
                PrintWriter pw = new PrintWriter(survey_file);
                pw.close();
            } catch (SftpException e) {
                e.printStackTrace();
            }
            channelSftp.exit();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            session.disconnect();
        }
        return files_sent;
    }

    @Override
    protected void onPostExecute(int[] files_sent) {
        //super.onPostExecute(files_sent);
        int sum = 0;
        for(int i=0; i<3; i++) sum = sum + files_sent[i];
        if (sum == 3) {
            Toast.makeText(ctx, "Files sent successfully!", Toast.LENGTH_LONG).show();
        }
        else if (files_sent[0] ==0){
                Toast.makeText(ctx, "GPS file did not send", Toast.LENGTH_SHORT).show();
            }

        if (files_sent[1] ==0){
            Toast.makeText(ctx, "App usage file not sent", Toast.LENGTH_SHORT).show();
        }
        if (files_sent[2]==0){
            Toast.makeText(ctx, "Survey file not sent", Toast.LENGTH_SHORT).show();
        }

        if (sum == 2) {
            Toast.makeText(ctx, "Only 2/3 files sent", Toast.LENGTH_LONG).show();
        }
        else if (sum == 1) {
            Toast.makeText(ctx, "Only 1/3 files sent", Toast.LENGTH_LONG).show();
        }
        else if (sum == 0) Toast.makeText(ctx, "Error: files did not send", Toast.LENGTH_LONG).show();
        }

    }

