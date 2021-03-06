/*
 * Copyright (C) 2012 Thierry-Dimitri Roy <thierryd@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.analysedesgeeks.android.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import roboguice.util.Ln;

import com.analysedesgeeks.android.Const;
import com.analysedesgeeks.android.rss.FeedItem;
import com.analysedesgeeks.android.service.ConnectionService;
import com.analysedesgeeks.android.service.DatabaseService;
import com.analysedesgeeks.android.service.DownloadService;
import com.analysedesgeeks.android.service.RssService;
import com.google.inject.Inject;

public class DownloadServiceImpl implements DownloadService {

	@Inject
	private DatabaseService databaseService;

	@Inject
	private ConnectionService connectionService;

	@Inject
	private RssService rssService;

	@Override
	public boolean downloadFeed() {
		boolean downloadSuccess = false;

		if (connectionService.isConnected()) {

			InputStream inputStream = null;
			try {
				final HttpGet httpget = new HttpGet(new URI(Const.FEED_URL));

				// Send GET request to URI
				final HttpResponse response = new DefaultHttpClient().execute(httpget);

				// Check if server response is valid
				final StatusLine status = response.getStatusLine();
				if (status.getStatusCode() != HttpStatus.SC_OK) {
					Ln.e("Erreur lors du tÚlÚchargement:%s,%s", status.getStatusCode(), status.getReasonPhrase());
				} else {

					// Extract content stream from HTTP response
					final HttpEntity entity = response.getEntity();
					inputStream = entity.getContent();

					final ByteArrayOutputStream content = new ByteArrayOutputStream();

					// Read response into a buffered stream
					int readBytes = 0;
					final byte[] sBuffer = new byte[512];
					while ((readBytes = inputStream.read(sBuffer)) != -1) {
						content.write(sBuffer, 0, readBytes);
					}

					// Return result from buffered stream
					final String dataAsString = new String(content.toByteArray());

					final List<FeedItem> syndFeed = rssService.parseRss(dataAsString);

					if (syndFeed != null) {
						databaseService.save(dataAsString);
						downloadSuccess = true;
					}
				}
			} catch (final Throwable t) {
				Ln.e(t);
			} finally {
				closeQuietly(inputStream);
			}

		}

		return downloadSuccess;
	}

	private void closeQuietly(final InputStream inputStream) {
		if (inputStream != null) {
			try {
				inputStream.close();
			} catch (final IOException e) {
				//ignore
			}
		}

	}

}
