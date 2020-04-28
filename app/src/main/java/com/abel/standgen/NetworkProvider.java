package com.abel.standgen;

import android.content.Context;
import android.os.AsyncTask;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

class NetworkProvider {
    final static NetworkProvider Instance = new NetworkProvider();

    private RequestQueue queue;
    private static final String REQUEST_TAG = "REQUEST";

    void initRequestQueue(Context context) {
        if (queue == null)
            queue = Volley.newRequestQueue(context);
    }

    void close() {
        queue.cancelAll(REQUEST_TAG);
    }

    void performRequest(Request req) {
        queue.add(req);
    }
}
