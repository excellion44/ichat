/**
 * Copyright (C) 2004-2011 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jivesoftware.sparkimpl.updater;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.jivesoftware.Spark;
import org.jivesoftware.resource.Default;
import org.jivesoftware.resource.Res;
import org.jivesoftware.resource.SparkRes;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.spark.SparkManager;
import org.jivesoftware.spark.component.ConfirmDialog;
import org.jivesoftware.spark.component.ConfirmDialog.ConfirmListener;
import org.jivesoftware.spark.component.TitlePanel;
import org.jivesoftware.spark.util.BrowserLauncher;
import org.jivesoftware.spark.util.ByteFormat;
import org.jivesoftware.spark.util.GraphicUtils;
import org.jivesoftware.spark.util.ModelUtil;
import org.jivesoftware.spark.util.SwingWorker;
import org.jivesoftware.spark.util.log.Log;
import org.jivesoftware.sparkimpl.settings.JiveInfo;
import org.jivesoftware.sparkimpl.settings.local.LocalPreferences;
import org.jivesoftware.sparkimpl.settings.local.SettingsManager;
import org.jxmpp.jid.impl.JidCreate;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.UIManager;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executors;

public class CheckUpdates
{




public CheckUpdates()
{
    String operSys = System.getProperty("os.name").toLowerCase();

    if (operSys.contains("win"))
    {
       // System.out.print(Default.getString(Default.VERSION_INFO)+"\n");

        String CurrentVersion = Default.getString(Default.VERSION_INFO); // текущая версия ядра

        /*Здесь скачиваем с сайта файл с актуальной версией в  System.getProperty("user.dir") c названием ver.txt*/


        try (BufferedInputStream inputStream = new BufferedInputStream(new URL("http://adm-sharya.ru/ichat/ver.txt").openStream());
             FileOutputStream fileOS = new FileOutputStream(System.getProperty("user.dir")+"\\ver.txt"))
        {

            byte data[] = new byte[1024];
            int byteContent;
            while ((byteContent = inputStream.read(data, 0, 1024)) != -1)
            {
                fileOS.write(data, 0, byteContent);
            }
        }
        catch (IOException e)
        {
            System.out.print(e.getMessage());
            return;
        }


        String FileServerVersion = System.getProperty("user.dir")+"\\ver.txt";

        Optional<String> line = null;

        try
        {
            line = Files.lines(Paths.get(FileServerVersion)).findFirst();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        //System.out.println(line.get()+"\n");

        String ServerVersion = line.get();

        if(!Objects.equals(CurrentVersion, ServerVersion))
        {

            int selectedOption = JOptionPane.showConfirmDialog(null, "Доступна новая версия программы \n Запустить обновление ?", System.getProperty("user.dir"), JOptionPane.YES_NO_OPTION);
            if (selectedOption == JOptionPane.YES_OPTION)
            {
                //System.out.print("Погнали обновляться");

                String homeDirectory = System.getProperty("user.dir")+"\\updater.jar";
                //homeDirectory = homeDirectory.replace("\\","\\\\");
                homeDirectory = "\""+homeDirectory+"\"";
                //System.out.print(homeDirectory);
                try
                {
                    Runtime.getRuntime().exec(String.format("cmd.exe /c "+homeDirectory));
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                System.exit(0);
            }
            else
            {

            }







        }
        else
        {
            System.out.print("Ниче не делаем");

        }
    }
    else if (operSys.contains("nix") || operSys.contains("nux") || operSys.contains("aix"))
    {
        System.out.print("Linux");
    }
    else if (operSys.contains("mac"))
    {
        System.out.print("Mac");
    }
    else if (operSys.contains("sunos"))
    {

    }
}






}
