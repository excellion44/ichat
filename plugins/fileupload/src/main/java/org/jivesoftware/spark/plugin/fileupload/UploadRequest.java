/**
 * Copyright (C) 2004-2011 Jive Software. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.spark.plugin.fileupload;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * An IQ packet that's a request for an upload slot
 */
public class UploadRequest extends IQ
{
    public static final String NAMESPACE = "urn:xmpp:http:upload:0";

    private String filename;
    private long filesize;

    public String getUrl = null;
    public String putUrl = null;

    public UploadRequest()
    {
        super( "request", NAMESPACE );
    }

    public UploadRequest(String filename, long filesize)
    {
        super( "request", NAMESPACE );
        this.filename = filename;
        this.filesize = filesize;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder( IQChildElementXmlStringBuilder buf )
    {
        buf.rightAngleBracket();
        buf.element("size", Long.toString( filesize ));
        buf.element("filename", filename);
        return buf;
    }

    public static class Provider extends IQProvider<UploadRequest>
    {
        public Provider()
        {
            super();
        }

        public UploadRequest parse( XmlPullParser parser, int i ) throws XmlPullParserException, IOException
        {
            final UploadRequest uploadRequest = new UploadRequest();

            boolean done = false;
            while ( !done )
            {
                int eventType = parser.next();

                if ( eventType == XmlPullParser.START_TAG )
                {
                    if ( parser.getName().equals( "put" ) )
                    {
                        uploadRequest.putUrl = parser.getAttributeValue(null, "url");
                    }
                    else if ( parser.getName().equals( "get" ) )
                    {
                        uploadRequest.getUrl = parser.getAttributeValue(null, "url");
                    }
                }

                else if ( eventType == XmlPullParser.END_TAG )
                {
                    if ( parser.getName().equals( "slot" ) )
                    {
                        done = true;
                    }
                }
            }

            return uploadRequest;
        }
    }
}
